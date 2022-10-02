package com.orange.common.springboot.autoconfigure.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.util.*;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;
import org.springframework.util.CollectionUtils;


/**
 * This is a {@link ProxySelector} implementation able to manage several {@link Proxy} depending on the uri scheme and host
 */
class MultiProxySelector extends ProxySelector {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiProxySelector.class);

    static class ProxyEntry {
        private final NetworkProxyProperties.ProxyServerConfig cfg;
        private final Proxy proxy;
        private final List<HostnameMatcher> positiveMatchers;
        private final List<HostnameMatcher> negativeMatchers;

        private ProxyEntry(NetworkProxyProperties.ProxyServerConfig cfg, Proxy proxy, List<HostnameMatcher> positiveMatchers, List<HostnameMatcher> negativeMatchers) {
            this.cfg = cfg;
            this.proxy = proxy;
            this.positiveMatchers = positiveMatchers;
            this.negativeMatchers = negativeMatchers;
        }

        Proxy getProxy() {
            return proxy;
        }

        boolean matches(String protocol, String host) {
            // test protocol matches
            if (!cfg.getForProtocols().contains(protocol)) {
                return false;
            }
            // test matchers
            if (positiveMatchers.isEmpty()) {
                return !negativeMatchers.stream().anyMatch(matcher -> matcher.matches(host));
            } else {
                return positiveMatchers.stream().anyMatch(matcher -> matcher.matches(host));
            }
        }

        @Override
        public String toString() {
            return cfg.toString();
        }
    }

    static class SchemeAndHost {
        final String protocol;
        final String host;

        SchemeAndHost(String protocol, String host) {
            this.protocol = protocol;
            this.host = host;
        }

        @Override
        public String toString() {
            return "{" +
                    "protocol='" + protocol + '\'' +
                    ", host='" + host + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SchemeAndHost that = (SchemeAndHost) o;
            return Objects.equals(protocol, that.protocol) &&
                    Objects.equals(host, that.host);
        }

        @Override
        public int hashCode() {
            return Objects.hash(protocol, host);
        }
    }

    private final List<ProxyEntry> proxies;

    private final boolean alwaysPrint;

    private final List<String> exclusions;

    private Map<SchemeAndHost, List<Proxy>> hostname2Proxies = new HashMap<>();

    private MultiProxySelector(List<ProxyEntry> proxies, boolean alwaysPrint,
                               List<String> exclusions) {
        this.proxies = proxies;
        this.alwaysPrint = alwaysPrint;
        this.exclusions = exclusions;
    }

    @Override
    public List<Proxy> select(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI can't be null.");
        }
        String protocol = uri.getScheme();
        String host = uri.getHost();

        if (host == null) {
            // This is a hack to ensure backward compatibility in two
            // cases: 1. hostnames contain non-ascii characters,
            // internationalized domain names. in which case, URI will
            // return null, see BugID 4957669; 2. Some hostnames can
            // contain '_' chars even though it's not supposed to be
            // legal, in which case URI will return null for getHost,
            // but not for getAuthority() See BugID 4913253
            String auth = uri.getAuthority();
            if (auth != null) {
                int i;
                i = auth.indexOf('@');
                if (i >= 0) {
                    auth = auth.substring(i + 1);
                }
                i = auth.lastIndexOf(':');
                if (i >= 0) {
                    auth = auth.substring(0, i);
                }
                host = auth;
            }
        }

        if (protocol == null || host == null) {
            throw new IllegalArgumentException("protocol = " + protocol + " host = " + host);
        }

        SchemeAndHost schemeAndHost = new SchemeAndHost(protocol, host);
        List<Proxy> proxiesList =
                hostname2Proxies.computeIfAbsent(schemeAndHost, this::doGetProxies);
        if (alwaysPrint && !CollectionUtils.contains(exclusions.iterator(), host)) {
            LOGGER.info("Proxies for [{}] : {}", schemeAndHost, proxiesList);
        }
        return proxiesList;
    }

    private List<Proxy> doGetProxies(SchemeAndHost schemeAndHost) {
        Proxy proxy = proxies.stream()
                .filter(e -> e.matches(schemeAndHost.protocol, schemeAndHost.host))
                .map(ProxyEntry::getProxy)
                .findFirst()
                .orElse(Proxy.NO_PROXY);
        LOGGER.info("Proxies for [{}] : {}", schemeAndHost, proxy);
        return Collections.singletonList(proxy);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        LOGGER.info("connect failed: {}", uri, ioe);
    }

    @Override
    public String toString() {
        return "MultiProxySelector{" +
                "proxies=" + proxies +
                '}';
    }

    static MultiProxySelector build(List<NetworkProxyProperties.ProxyServerConfig> proxies,
                                    boolean alwaysPrint, List<String> exclusions) {
        List<ProxyEntry> proxyEntries = new ArrayList<>();
        for (int i = 0; i < proxies.size(); i++) {
            NetworkProxyProperties.ProxyServerConfig cfg = proxies.get(i);

            if (cfg.getHost() == null || cfg.getHost().length() == 0) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "].host can't be null or empty.");
            }

            if (cfg.getPort() == null) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "].port can't be null.");
            }

            int countMatchers = (cfg.getForHosts().isEmpty() ? 0 : 1) + (cfg.getNotForHosts().isEmpty() ? 0 : 1);
            if (countMatchers == 0) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "] must define either '.forHosts' or '.notForHosts' matchers in configuration.");
            } else if (countMatchers > 1) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "] you can't specify both '.forHosts' and '.notForHosts' matchers in configuration.");
            }

            // parse matchers
            List<HostnameMatcher> positiveMatchers;
            try {
                positiveMatchers = cfg.getForHosts().stream().map(HostnameMatcher::parse).collect(Collectors.toList());
            } catch (PatternSyntaxException pte) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "].for-hosts contains an invalid pattern.", pte);
            }
            List<HostnameMatcher> negativeMatchers;
            try {
                negativeMatchers = cfg.getNotForHosts().stream().map(HostnameMatcher::parse).collect(Collectors.toList());
            } catch (PatternSyntaxException pte) {
                throw new IllegalArgumentException("network.proxy.servers[" + i + "].not-for-hosts contains an invalid pattern.", pte);
            }

            // make proxy
            Proxy proxy = new Proxy(cfg.getType() == NetworkProxyProperties.ProxyServerConfig.Type.http ? Proxy.Type.HTTP : Proxy.Type.SOCKS, new InetSocketAddress(cfg.getHost(), cfg.getPort()));
            proxyEntries.add(new ProxyEntry(cfg, proxy, positiveMatchers, negativeMatchers));
        }

        return new MultiProxySelector(proxyEntries, alwaysPrint, exclusions);
    }
}
