package com.hillcommerce.framework.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ClientIpResolverTest {

    private ClientIpResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientIpResolver(Set.of(
            "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16", "127.0.0.0/8"));
    }

    @Test
    void shouldReturnPublicIpFromXForwardedForWhenRemoteAddrIsTrustedProxy() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.5, 10.0.0.2, 192.168.1.1");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.5", ip);
    }

    @Test
    void shouldIgnoreXForwardedForWhenRemoteAddrIsUntrusted() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("203.0.113.99");
        when(request.getHeader("X-Forwarded-For")).thenReturn("1.2.3.4");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.99", ip);
    }

    @Test
    void shouldFallbackToXRealIpWhenXForwardedForMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.10");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.10", ip);
    }

    @Test
    void shouldFallbackToRemoteAddrWhenBothHeadersMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);

        String ip = resolver.resolve(request);
        assertEquals("10.0.0.1", ip);
    }

    @Test
    void shouldFilterOutUnknownTokenInXForwardedFor() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown, 203.0.113.5");

        String ip = resolver.resolve(request);
        assertEquals("203.0.113.5", ip);
    }
}
