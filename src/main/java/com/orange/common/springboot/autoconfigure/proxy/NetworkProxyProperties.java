package com.orange.common.springboot.autoconfigure.proxy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

@ConfigurationProperties(prefix = "network.proxy")
@Validated
public class NetworkProxyProperties implements Validator  {
    /**
     * Whether to enable network proxy auto configuration.
     */
    private boolean enabled = true;

    /**
     * Whether to enable the print of the proxy used every time.
     */
    private boolean alwaysPrint = false;

    /**
     * Explicit network proxy servers configuration
     */
    @Valid
    private List<ProxyServerConfig> servers = Collections.emptyList();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ProxyServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ProxyServerConfig> servers) {
        this.servers = servers;
    }

    public boolean isAlwaysPrint() {
        return alwaysPrint;
    }

    public void setAlwaysPrint(boolean alwaysPrint) {
        this.alwaysPrint = alwaysPrint;
    }

    @Override
    public String toString() {
        return "NetworkProxyProperties{" +
                "enabled=" + enabled +
                ", alwaysPrint= " +alwaysPrint +
                ", servers=" + servers +
                '}';
    }

    @Validated
    public static class ProxyServerConfig {
        enum Type {
            http, socks
        }
        /**
         * The proxy type ({@code http} or {@code socks}). Default: {@code http}.
         */
        private Type type = Type.http;
        /**
         * The proxy host
         */
        @NotEmpty
        private String host;
        /**
         * The proxy port
         */
        @NotNull
        private Integer port;
        /**
         * The proxy username
         */
        private String username;
        /**
         * The proxy password
         */
        private String password;
        /**
         * For hosts matchers
         */
        private List<String> forHosts = Collections.emptyList();
        /**
         * Not for hosts matchers
         */
        private List<String> notForHosts = Collections.emptyList();
        /**
         * Protocols. Default: {@code ["http", "https", "ftp"]}
         */
        private List<String> forProtocols = Arrays.asList("http", "https", "ftp");

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public List<String> getNotForHosts() {
            return notForHosts;
        }

        public void setNotForHosts(List<String> notForHosts) {
            this.notForHosts = notForHosts;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public List<String> getForHosts() {
            return forHosts;
        }

        public void setForHosts(List<String> forHosts) {
            this.forHosts = forHosts;
        }

        public List<String> getForProtocols() {
            return forProtocols;
        }

        public void setForProtocols(List<String> forProtocols) {
            this.forProtocols = forProtocols;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "ProxyServerConfig{" +
                    "type=" + type +
                    ", host='" + host + '\'' +
                    ", port=" + port +
                    ", username='" + username + '\'' +
                    ", password='" + (password == null ? "(none)" : "***") + '\'' +
                    ", forHosts=" + forHosts +
                    ", notForHosts=" + notForHosts +
                    ", forProtocols=" + forProtocols +
                    '}';
        }
    }

    // ================================================================================================================
    // === Validator impl
    // ================================================================================================================

    @Override
    public boolean supports(Class<?> clazz) {
        return clazz.equals(NetworkProxyProperties.class);
    }

    @Override
    public void validate(Object target, Errors errors) {
        NetworkProxyProperties properties = (NetworkProxyProperties) target;
        List<NetworkProxyProperties.ProxyServerConfig> proxies = properties.getServers();
        for (int i = 0; i < proxies.size(); i++) {
            NetworkProxyProperties.ProxyServerConfig cfg = proxies.get(i);

            int countMatchers = (cfg.getForHosts().isEmpty() ? 0 : 1) + (cfg.getNotForHosts().isEmpty() ? 0 : 1);
            if(countMatchers == 0) {
                errors.rejectValue("servers[" + i + "]", "nomatcher", "you must specify either 'forHosts' or 'notForHosts' matchers");
            } else if(countMatchers > 1) {
                errors.rejectValue("servers[" + i + "]", "toomanymatchers", "you can't specify both 'forHosts' and 'notForHosts' matchers");
            }

            // check patterns
            for(int j=0; j<cfg.getForHosts().size(); j++) {
                if (cfg.getForHosts().get(j) == null || cfg.getForHosts().get(j).isEmpty()) {
                    errors.rejectValue("servers[" + i + "].forHosts[" + j + "]", "NotEmpty", "can't be empty");
                } else {
                    try {
                        HostnameMatcher.parse(cfg.getForHosts().get(j));
                    } catch (PatternSyntaxException pte) {
                        errors.rejectValue("servers[" + i + "].forHosts[" + j + "]", "invalid", "can't be parsed as a valid regexp");
                    }
                }
            }

            for(int j=0; j<cfg.getNotForHosts().size(); j++) {
                if (cfg.getNotForHosts().get(j) == null || cfg.getNotForHosts().get(j).isEmpty()) {
                    errors.rejectValue("servers[" + i + "].notForHosts["+j+"]", "NotEmpty", "can't be empty");
                } else {
                    try {
                        HostnameMatcher.parse(cfg.getNotForHosts().get(j));
                    } catch (PatternSyntaxException pte) {
                        errors.rejectValue("servers[" + i + "].notForHosts["+j+"]", "invalid", "can't be parsed as a valid regexp");
                    }
                }
            }
        }
    }
}
