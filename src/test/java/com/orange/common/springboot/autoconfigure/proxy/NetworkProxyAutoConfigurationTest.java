package com.orange.common.springboot.autoconfigure.proxy;

import org.assertj.core.api.Condition;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.bind.validation.BindValidationException;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@Ignore
public class NetworkProxyAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NetworkProxyAutoConfiguration.class));

    @Test
    public void basic_conf_should_work() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=*"
        )
                .run((context) -> {
                    assertThat(context.isRunning()).isTrue();
                    assertThat(ProxySelector.getDefault()).isInstanceOf(MultiProxySelector.class);
                });
    }

    @Test
    public void basic_conf2_should_work() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.username=login",
                "network.proxy.servers.0.password=password",
                "network.proxy.servers.0.not-for-hosts.0=intranet.fr"
        )
                .run((context) -> {
                    assertThat(context.isRunning()).isTrue();
                    assertThat(ProxySelector.getDefault()).isInstanceOf(MultiProxySelector.class);
                });
    }

    @Test
    public void uppercase_type_should_work() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.type=HTTP",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=*"
        )
                .run((context) -> {
                    assertThat(context.isRunning()).isTrue();
                    assertThat(ProxySelector.getDefault()).isInstanceOf(MultiProxySelector.class);
                });
    }

    @Test
    public void invalid_type_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.type=nosuchtype",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=*"
        )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    // ConversionFailedException
                });
    }

    @Test
    public void missing_host_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=*"
        )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    assertIsValidationError(failure, "network.proxy", "servers[0].host", "NotEmpty");
                });
    }

    @Test
    public void missing_port_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.for-hosts.0=*"
        )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    assertIsValidationError(failure, "network.proxy", "servers[0].port", "NotNull");
                });
    }

    @Test
    public void invalid_regex_matcher_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",

                "network.proxy.servers.0.host=INTRANET",
                "network.proxy.servers.0.port=3128",
                // faulty regex
                "network.proxy.servers.0.for-hosts.0=/+/",
                "network.proxy.servers.0.for-hosts.1=*.intranet.fr",
                "network.proxy.servers.0.for-hosts.2=some.host.on.intranet"
                )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    assertIsValidationError(failure, "network.proxy", "servers[0].forHosts[0]", "invalid");
                });
    }

    @Test
    public void missing_matchers_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",

                "network.proxy.servers.0.host=INTRANET",
                "network.proxy.servers.0.port=3128"
        )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    assertIsValidationError(failure, "network.proxy", "servers[0]", "nomatcher");
                });
    }

    @Test
    public void two_matchers_should_fail() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",
                "network.proxy.servers.0.host=proxyhost",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=*",
                "network.proxy.servers.0.not-for-hosts.0=*"
        )
                .run((context) -> {
                    Throwable failure = context.getStartupFailure();
                    assertIsBeanCreationException(failure, "networkProxyAutoConfiguration");
                    assertIsValidationError(failure, "network.proxy", "servers[0]", "toomanymatchers");
                });
    }

    @Test
    public void several_proxies_should_work() {
        this.contextRunner.withPropertyValues(
                "network.proxy.enable=true",

                "network.proxy.servers.0.host=INTRANET",
                "network.proxy.servers.0.port=3128",
                "network.proxy.servers.0.for-hosts.0=/10\\.236\\.\\d+\\.\\d+/",
                "network.proxy.servers.0.for-hosts.1=*.intranet.fr",
                "network.proxy.servers.0.for-hosts.2=app.intranet",

                "network.proxy.servers.1.host=INTERNET",
                "network.proxy.servers.1.port=3128",
                "network.proxy.servers.1.not-for-hosts.0=/10\\.99\\.\\d+\\.\\d+/",
                "network.proxy.servers.1.not-for-hosts.1=localhost",
                "network.proxy.servers.1.not-for-hosts.2=127.0.0.1"
        )
                .run((context) -> {
                    assertThat(context.isRunning()).isTrue();
                    ProxySelector selector = ProxySelector.getDefault();
                    assertThat(selector).isInstanceOf(MultiProxySelector.class);

                    // test intranet addresses
                    assertThat(selector.select(new URI("http://host1.intranet.fr/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTRANET:3128"));
                    assertThat(selector.select(new URI("socket://host1.intranet.fr/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.DIRECT, null));
                    assertThat(selector.select(new URI("http://10.236.1.1/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTRANET:3128"));
                    assertThat(selector.select(new URI("http://some.app.intranet/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTRANET:3128"));
                    assertThat(selector.select(new URI("http://app.intranet/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTRANET:3128"));
                    // test local addresses
                    assertThat(selector.select(new URI("https://localhost/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.DIRECT, null));
                    assertThat(selector.select(new URI("https://127.0.0.1/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.DIRECT, null));
                    assertThat(selector.select(new URI("https://10.99.1.1/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.DIRECT, null));
                    // test internet addresses
                    assertThat(selector.select(new URI("https://www.google.com/"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTERNET:3128"));
                    assertThat(selector.select(new URI("http://www.google.com/"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTERNET:3128"));
                    assertThat(selector.select(new URI("http://172.3.12.5/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTERNET:3128"));
                    assertThat(selector.select(new URI("http://someapp.intranet/a/b/c"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.HTTP, "INTERNET:3128"));
                    assertThat(selector.select(new URI("socket://www.google.com/"))).hasSize(1)
                            .element(0).has(proxy(Proxy.Type.DIRECT, null));

                });
    }

    private Condition<Proxy> proxy(Proxy.Type type, String address) {
        return new Condition<>(proxy -> {
            assertThat(proxy).extracting("type").containsExactly(type);
            if (address != null) {
                assertThat(proxy.address().toString()).isEqualTo(address);
            }
            return true;
        }, "proxy condition");
    }

    void assertIsBeanCreationException(Throwable error, String beanName) {
        assertThat(error).isInstanceOf(BeanCreationException.class)
                .extracting("beanName").containsExactly(beanName);
    }

    void assertIsValidationError(Throwable error, String objectName, String field, String code) {
        while (error != null && !(error instanceof BindValidationException) && error.getCause() != error) {
            error = error.getCause();
        }
        assertThat(error).isInstanceOf(BindValidationException.class);
        BindValidationException bve = (BindValidationException) error;
        for (ObjectError err : bve.getValidationErrors().getAllErrors()) {
            if (err instanceof FieldError) {
                if (err.getObjectName().equals(objectName) && ((FieldError) err).getField().equals(field)) {
                    assertThat(err.getCode()).isEqualTo(code);
                    return;
                }
            }
        }
        fail("expected field " + field + " error not found");
    }

}