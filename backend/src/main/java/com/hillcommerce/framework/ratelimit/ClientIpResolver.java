package com.hillcommerce.framework.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final List<String> NON_ROUTABLE_TOKENS = List.of(
        "unknown", "localhost", "127.0.0.1", "::1");

    private final Set<String> trustedProxyCidrs;

    public ClientIpResolver() {
        this(Set.of("10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8"));
    }

    ClientIpResolver(Set<String> trustedProxyCidrs) {
        this.trustedProxyCidrs = trustedProxyCidrs;
    }

    public String resolve(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (isTrustedProxy(remoteAddr)) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                for (String token : forwardedFor.split(",")) {
                    String ip = token.trim();
                    if (isPlausibleClientIp(ip) && !isPrivateAddress(ip)) {
                        return ip;
                    }
                }
            }

            String realIp = request.getHeader("X-Real-IP");
            if (realIp != null && !realIp.isBlank()) {
                String ip = realIp.trim();
                if (isPlausibleClientIp(ip)) {
                    return ip;
                }
            }
        }

        return remoteAddr;
    }

    private boolean isTrustedProxy(String ip) {
        for (String cidr : trustedProxyCidrs) {
            if (ipInCidr(ip, cidr)) {
                return true;
            }
        }
        return false;
    }

    private boolean isPlausibleClientIp(String ip) {
        for (String token : NON_ROUTABLE_TOKENS) {
            if (token.equalsIgnoreCase(ip)) {
                return false;
            }
        }
        return !ip.contains(" ") && !ip.contains("\t") && !ip.isEmpty();
    }

    private boolean isPrivateAddress(String ip) {
        if (ip.startsWith("10.") || ip.startsWith("192.168.") || ip.startsWith("127.")) {
            return true;
        }
        if (ip.startsWith("172.")) {
            String[] parts = ip.split("\\.");
            if (parts.length >= 2) {
                try {
                    int second = Integer.parseInt(parts[1]);
                    if (second >= 16 && second <= 31) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean ipInCidr(String ip, String cidr) {
        int slash = cidr.indexOf('/');
        if (slash < 0) {
            return ip.equals(cidr);
        }
        String prefix = cidr.substring(0, slash);
        int maskLen;
        try {
            maskLen = Integer.parseInt(cidr.substring(slash + 1));
        } catch (NumberFormatException e) {
            return false;
        }

        long ipLong = ipv4ToLong(ip);
        long prefixLong = ipv4ToLong(prefix);
        if (ipLong < 0 || prefixLong < 0) {
            return false;
        }
        long mask = maskLen == 0 ? 0 : (0xFFFFFFFFL << (32 - maskLen)) & 0xFFFFFFFFL;
        return (ipLong & mask) == (prefixLong & mask);
    }

    private long ipv4ToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return -1;
        }
        try {
            long result = 0;
            for (int i = 0; i < 4; i++) {
                int octet = Integer.parseInt(parts[i]);
                if (octet < 0 || octet > 255) {
                    return -1;
                }
                result = (result << 8) | octet;
            }
            return result & 0xFFFFFFFFL;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
