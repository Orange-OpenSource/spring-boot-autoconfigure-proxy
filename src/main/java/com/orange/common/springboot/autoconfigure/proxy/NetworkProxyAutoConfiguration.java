package com.orange.common.springboot.autoconfigure.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import javax.annotation.PostConstruct;
import java.net.Authenticator;
import java.net.ProxySelector;

@Configuration
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@AutoConfigureAfter(PropertyPlaceholderAutoConfiguration.class)
@ConditionalOnProperty(prefix = "network.proxy", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(NetworkProxyProperties.class)
public class NetworkProxyAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkProxyAutoConfiguration.class);
    private static final String[] PROTOCOLS = {"http", "https", "ftp"};

    private final NetworkProxyProperties properties;

    public NetworkProxyAutoConfiguration(NetworkProxyProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void setupProxyConfiguration() {
        MultiServerAuthenticator msa = new MultiServerAuthenticator();

        if (!properties.getServers().isEmpty()) {
            // CASE 1: explicit proxies configuration
            LOGGER.info("Configuring proxies from Spring Boot configuration");

            // install proxy selector
            ProxySelector.setDefault(MultiProxySelector.build(properties.getServers(),
                    properties.isAlwaysPrint()));

            // set password authentication for every proxy that need one
            for (NetworkProxyProperties.ProxyServerConfig cfg : properties.getServers()) {
                if (cfg.getUsername() != null && cfg.getPassword() != null) {
                    msa.add(cfg.getHost() + ":" + cfg.getPort(), cfg.getUsername(), cfg.getPassword());
                }
            }
        } else {
            for (String protocol : PROTOCOLS) {
                ProxySettingsFromEnv proxySettings = ProxySettingsFromEnv.read(protocol);
                if (proxySettings != null) {
                    // CASE 2: auto-conf from ENV
                    LOGGER.info("Configuring proxy for {} from env '{}': {}", protocol, proxySettings.getEnvName(), proxySettings);

                    // set password authent if specified
                    if (proxySettings.getUsername() != null && proxySettings.getPassword() != null) {
                        msa.add(proxySettings.getHost() + ":" + proxySettings.getPort(), proxySettings.getUsername(), proxySettings.getPassword());
                    }

                    // set proxy properties
                    System.setProperty(protocol + ".proxyHost", proxySettings.getHost());
                    System.setProperty(protocol + ".proxyPort", String.valueOf(proxySettings.getPort()));
                    if (proxySettings.getNoProxyHosts() != null && proxySettings.getNoProxyHosts().length > 0) {
                        System.setProperty(protocol + ".nonProxyHosts", String.join("|", proxySettings.getNoProxyHosts()));
                    }
                } else {
                    // CASE 3: auto-conf from Java properties (support http.proxyUser & http.proxyPassword)
                    String host = System.getProperty(protocol + ".proxyHost");
                    String port = System.getProperty(protocol + ".proxyPort");
                    String username = System.getProperty(protocol + ".proxyUser");
                    String password = System.getProperty(protocol + ".proxyPassword");
                    if (host != null && port != null && username != null && password != null) {
                        LOGGER.info("Configuring proxy authent for {} from Java properties '{}' & '{}'", protocol, protocol + ".proxyUser", protocol + ".proxyPassword");
                        msa.add(host + ":" + port, username, password);
                    } else {
                        // no proxy configuration
                        LOGGER.info("No proxy configuration found for {}", protocol);
                    }
                }
            }
        }

        // install default authenticator (if not empty)
        if (msa.size() > 0) {
            // see: https://www.oracle.com/technetwork/java/javase/8u111-relnotes-3124969.html
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "");
            Authenticator.setDefault(msa);
        }
    }
}
