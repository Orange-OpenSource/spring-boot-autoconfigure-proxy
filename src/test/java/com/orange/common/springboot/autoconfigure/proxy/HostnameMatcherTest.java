package com.orange.common.springboot.autoconfigure.proxy;

import org.junit.Test;

import static com.orange.common.springboot.autoconfigure.proxy.HostnameMatcher.parse;
import static org.assertj.core.api.Assertions.assertThat;

public class HostnameMatcherTest {
    @Test
    public void parse_domain_or_hostname_should_work() {
        HostnameMatcher matcher = parse("orange.com");
        assertThat(matcher)
                .isInstanceOf(HostnameMatcher.HostOrDomainMatcher.class);
        assertThat(matcher.matches("orange.com")).isTrue();
        assertThat(matcher.matches("portal.orange.com")).isTrue();
        assertThat(matcher.matches("myorange.com")).isFalse();
    }

    @Test
    public void parse_domain_should_work() {
        HostnameMatcher matcher = parse(".orange.com");
        assertThat(matcher)
                .isInstanceOf(HostnameMatcher.HostOrDomainMatcher.class);
        assertThat(matcher.matches("orange.com")).isFalse();
        assertThat(matcher.matches("portal.orange.com")).isTrue();
        assertThat(matcher.matches("myorange.com")).isFalse();
    }

    @Test
    public void parse_matcher_with_leading_star_should_work() {
        HostnameMatcher matcher = parse("*.orange.com");
        assertThat(matcher)
                .isInstanceOf(HostnameMatcher.PatternMatcher.class)
                .extracting("pattern")
                .extracting("pattern")
                .containsExactly(".*\\Q.orange.com\\E");
        assertThat(matcher.matches("orange.com")).isFalse();
        assertThat(matcher.matches("portal.orange.com")).isTrue();
        assertThat(matcher.matches("myorange.com")).isFalse();
    }

    @Test
    public void parse_matcher_with_trailing_star_should_work() {
        HostnameMatcher matcher = parse("*.orange.*");
        assertThat(matcher)
                .isInstanceOf(HostnameMatcher.PatternMatcher.class)
                .extracting("pattern")
                .extracting("pattern")
                .containsExactly(".*\\Q.orange.\\E.*");
        assertThat(matcher.matches("orange.com")).isFalse();
        assertThat(matcher.matches("portal.orange.com")).isTrue();
        assertThat(matcher.matches("portal.orange.fr")).isTrue();
        assertThat(matcher.matches("myorange.com")).isFalse();
    }

    @Test
    public void parse_regex_matcher_should_work() {
        assertThat(parse("\\.*\\.orange\\.com\\"))
                .isInstanceOf(HostnameMatcher.PatternMatcher.class)
                .extracting("pattern")
                .extracting("pattern")
                .containsExactly(".*\\.orange\\.com");
    }
}