# Spring Boot Auto Configure: Proxy
[![Build Status](https://travis-ci.org/Orange-OpenSource/spring-boot-autoconfigure-proxy.svg?branch=master)](https://travis-ci.org/Orange-OpenSource/spring-boot-autoconfigure-proxy)
<!-- [![Download](https://api.bintray.com/packages/orange-opensource/maven/spring-boot-autoconfigure-proxy/images/download.svg) ](https://bintray.com/orange-opensource/maven/spring-boot-autoconfigure-proxy/_latestVersion) -->

This Spring Boot library provides network proxy auto-configuration.


<a name="purpose"/>

## Purpose

There are several caveats when managing network proxies in Java:
1. authentication management \
    _Java doesn't support any property such as `(http|https|ftp).proxyHost` or `(http|https|ftp).proxyPort` to define the username & 
    password to use with an http proxy that requires basic authentication_
2. multiple proxies \
    _if you run you app in an environment with **several** proxies (say one for the intranet, one for the internet),
    Java properties are not enough_
3. no standard environment support \
    _on Linux, `http_proxy`, `https_proxy`, `ftp_proxy` and `no_proxy` environment variables are quite a standard...
    too bad Java doesn't support them natively_

Well, this Spring Boot auto-configurer is all about those 3 points.

From the running context, it will try to discover and setup network proxy(ies) in the following way and order:
1. _explicit_ configuration from **Spring Boot configuration** (`yaml`, properties or else),
2. _implicit_ configuration from **environment variables**,
3. _implicit_ configuration from **Java properties**.

<a name="usage"/>


## Usage

### Include it in your project

Maven style (`pom.xml`):

```xml
<repositories>
  <!-- add the Orange GitHub repository -->
  <repository>
    <id>github-orange</id>
    <url>https://maven.pkg.github.com/Orange-OpenSource/spring-boot-autoconfigure</url>
    <releases>
      <enabled>true</enabled>
    </releases>
    <snapshots>
      <enabled>false</enabled>
    </snapshots>
  </repository>
</repositories>

<dependencies>
  ...
  <dependency>
    <groupId>com.orange.common</groupId>
    <artifactId>spring-boot-autoconfigure-proxy</artifactId>
    <version>1.0.2</version>
  </dependency>
  ...
</dependencies>
```

### Using environment variables

Proxy configuration can be implicitly set using the following **environment variables**:

variable                       | description
------------------------------ | ------------------------------------------------
`http_proxy` or `HTTP_PROXY`   | The proxy URL to use for `http` connections. Format: `<protocol>://<proxy_host>:<proxy_port>` or `<protocol>://<user>:<password>@<proxy_host>:<proxy_port>`
`https_proxy` or `HTTPS_PROXY` | The proxy URL to use for `https` connections. Format: `<protocol>://<proxy_host>:<proxy_port>` or `<protocol>://<user>:<password>@<proxy_host>:<proxy_port>`
`ftp_proxy` or `FTP_PROXY`     | The proxy URL to use for `ftp` connections. Format: `<protocol>://<proxy_host>:<proxy_port>` or `<protocol>://<user>:<password>@<proxy_host>:<proxy_port>`
`no_proxy` or `NO_PROXY`       | Comma-separated list of domain extensions proxy should _not_ be used for.

See: [wget documentation](https://www.gnu.org/software/wget/manual/html_node/Proxies.html)


### Using Java properties

Proxy configuration can be implicitly set using the following **Java properties**:

property              | description
--------------------- | ------------------------------------------------
`http.proxyHost`      | Standard Java property defining the proxy host to use for `http` connections.
`http.proxyPort`      | Standard Java property defining the proxy port to use for `http` connections.
`http.nonProxyHosts`  | Standard Java property defining list of domain extensions proxy should _not_ be used for `http` connections.
`http.proxyUser`      | Non-standard Java property defining the basic authent username to use for `http` connections.
`http.proxyPassword`  | Non-standard Java property defining the basic authent password to use for `http` connections.
`https.proxyHost`      | Standard Java property defining the proxy host to use for `http`s connections.
`https.proxyPort`      | Standard Java property defining the proxy port to use for `https` connections.
`https.nonProxyHosts`  | Standard Java property defining list of domain extensions proxy should _not_ be used for `https` connections.
`https.proxyUser`      | Non-standard Java property defining the basic authent username to use for `https` connections.
`https.proxyPassword`  | Non-standard Java property defining the basic authent password to use for `https` connections.
`ftp.proxyHost`      | Standard Java property defining the proxy host to use for `ftp` connections.
`ftp.proxyPort`      | Standard Java property defining the proxy port to use for `ftp` connections.
`ftp.nonProxyHosts`  | Standard Java property defining list of domain extensions proxy should _not_ be used for `ftp` connections.
`ftp.proxyUser`      | Non-standard Java property defining the basic authent username to use for `ftp` connections.
`ftp.proxyPassword`  | Non-standard Java property defining the basic authent password to use for `ftp` connections.

See: [Java Networking and Proxies documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html)


### Using Spring Boot configuration

Unless you have multiple proxy servers to manage, `spring-boot-autoconfigure-proxy` can always be used either the 
**environment variables** way or the **Java properties** way.

But in the other case or if you just prefer controlling your configuration, go the **Spring Boot configuration** way! 

Here is a configuration example with a proxy for intranet addresses, and another one for internet addresses:

```yaml
network:
  proxy:
    enable: true # allows disabling auto-config; enabled by default

    # explicit list of proxy servers with settings
    servers:
      # 1: the intranet proxy (doesn't require any authentication)
      -
        host: intranet.proxy.acme.com
        port: 3128
        # list of hostname matchers
        for-hosts:
          # support basic hostname
          - portal.acme.com
          # or domain name
          - .intranet.acme.com
          # or wildcards
          - "*.intranet.acme.*"
          # or even regular expressions
          - /10\.236\.\d+\.\d+/

      # 2: the internet proxy (requires an authentication)
      -
        host: internet.proxy.acme.com
        port: 8080
        username: pismy
        password: let.me.out
        # list of non-hostname matchers
        not-for-hosts:
          - localhost
          - 127.0.0.1
          - /10\.99\.\d+\.\d+/
```

When the application is trying to access a network resource, the URI hostname will be tested against every configured proxy
one by one, if none matches then a direct connection will be used (no proxy).

Examples:
- `https://www.google.com` would match the **internet** proxy in the above configuration,
- `http://10.99.101.5/path/to/a/resource` would'nt match any configured proxy and would use direct connection (matches the last regex non-matcher from internet proxy),
- `http://billing.intranet.acme.fr/api` would match the **intranet** proxy (matches the 3rd wildcard matcher).

<a name="license"/>

## License

This code is under [Apache-2.0 License](LICENSE.txt)

