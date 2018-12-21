package com.orange.common.springboot.autoconfigure.proxy;

import java.util.regex.Pattern;

public abstract class HostnameMatcher {

    public abstract boolean matches(String hostname);

    public static HostnameMatcher parse(String matcher) {
        if (matcher.startsWith("/") && matcher.endsWith("/")) {
            // the matcher is a regexp
            return new PatternMatcher(Pattern.compile(matcher.substring(1, matcher.length() - 1)));
        } else {
            // replace '*' wilcards, quote all the rest
            StringBuilder regex = new StringBuilder(matcher.length());
            int cur = 0;
            int next = 0;
            while (next < matcher.length() && (next = matcher.indexOf('*', cur)) >= 0) {
                if (next - cur > 0) {
                    regex.append(Pattern.quote(matcher.substring(cur, next)));
                }
                regex.append(".*");
                cur = next + 1;
            }
            if (cur == 0) {
                // no star in the matcher
                return new HostOrDomainMatcher(matcher);
            } else {
                // append tail
                if (matcher.length() - cur > 0) {
                    regex.append(Pattern.quote(matcher.substring(cur)));
                }
                return new PatternMatcher(Pattern.compile(regex.toString()));
            }
        }
    }

    static class HostOrDomainMatcher extends HostnameMatcher {
        private final String hostOrDomain;

        private HostOrDomainMatcher(String hostOrDomain) {
            this.hostOrDomain = hostOrDomain;
        }

        @Override
        public boolean matches(String hostname) {
            return hostname.endsWith(hostOrDomain)
                    && (
                    hostOrDomain.startsWith(".") // hostOrDomain is explicitly a domain
                            || hostname.length() == hostOrDomain.length() // hostOrDomain is a hostname
                            || hostname.charAt(hostname.length() - hostOrDomain.length() - 1) == '.' // hostOrDomain is maybe a domain name without leading '.'
            );
        }
    }

    static class PatternMatcher extends HostnameMatcher {
        private final Pattern pattern;

        private PatternMatcher(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String hostname) {
            return pattern.matcher(hostname).matches();
        }
    }
}
