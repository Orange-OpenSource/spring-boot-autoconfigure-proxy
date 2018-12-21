package com.orange.common.springboot.autoconfigure.proxy;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProxySettingsFromEnvTest {
    @Test
    public void simple_http_proxy_should_be_parsed() {
        ProxySettingsFromEnv proxy = ProxySettingsFromEnv.parse("http", "http://proxy:8080", null);
        assertThat(proxy).isNotNull();
        assertThat(proxy.getProtocol()).isEqualTo("http");
        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getForProtocol()).isEqualTo("http");
        assertThat(proxy.getUsername()).isNull();
        assertThat(proxy.getPassword()).isNull();
        assertThat(proxy.getNoProxyHosts()).isNullOrEmpty();
    }

    @Test
    public void simple_socks_proxy_should_be_parsed() {
        ProxySettingsFromEnv proxy = ProxySettingsFromEnv.parse("http", "socks://proxy:8080", null);
        assertThat(proxy).isNotNull();
        assertThat(proxy.getProtocol()).isEqualTo("socks");
        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getForProtocol()).isEqualTo("http");
        assertThat(proxy.getUsername()).isNull();
        assertThat(proxy.getPassword()).isNull();
        assertThat(proxy.getNoProxyHosts()).isNullOrEmpty();
    }

    @Test
    public void simple_socks5_proxy_should_be_parsed() {
        ProxySettingsFromEnv proxy = ProxySettingsFromEnv.parse("http", "socks5://proxy:8080", null);
        assertThat(proxy).isNotNull();
        assertThat(proxy.getProtocol()).isEqualTo("socks5");
        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getForProtocol()).isEqualTo("http");
        assertThat(proxy.getUsername()).isNull();
        assertThat(proxy.getPassword()).isNull();
        assertThat(proxy.getNoProxyHosts()).isNullOrEmpty();
    }

    @Test
    public void http_with_authent_proxy_should_be_parsed() {
        ProxySettingsFromEnv proxy = ProxySettingsFromEnv.parse("http", "http://user:password@proxy:8080", null);
        assertThat(proxy).isNotNull();
        assertThat(proxy.getProtocol()).isEqualTo("http");
        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getForProtocol()).isEqualTo("http");
        assertThat(proxy.getUsername()).isEqualTo("user");
        assertThat(proxy.getPassword()).isEqualTo("password");
        assertThat(proxy.getNoProxyHosts()).isNullOrEmpty();
    }

    @Test
    public void http_proxy_with_nohosts_should_be_parsed() {
        ProxySettingsFromEnv proxy = ProxySettingsFromEnv.parse("http", "http://proxy:8080", "localhost, 127.0.0.1, *.intranet.fr");
        assertThat(proxy).isNotNull();
        assertThat(proxy.getProtocol()).isEqualTo("http");
        assertThat(proxy.getHost()).isEqualTo("proxy");
        assertThat(proxy.getPort()).isEqualTo(8080);
        assertThat(proxy.getForProtocol()).isEqualTo("http");
        assertThat(proxy.getUsername()).isNull();
        assertThat(proxy.getPassword()).isNull();
        assertThat(proxy.getNoProxyHosts()).containsExactly("localhost", "127.0.0.1", "*.intranet.fr");
    }

}