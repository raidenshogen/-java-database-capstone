package org.shopwave.smartclinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limits repeated form-login attempts from one client address.
 *
 * Spring Security 7.1.2 does not provide the requested
 * {@code RequestRateLimiterConfigurer}, so this filter applies the same
 * protection before the authentication filter.
 */
public class LoginRateLimitingFilter extends OncePerRequestFilter {

    private static final int MAX_ATTEMPTS = 10;
    private static final long WINDOW_MILLIS = Duration.ofMinutes(1).toMillis();

    private final ConcurrentHashMap<String, AttemptWindow> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        if (!HttpMethod.POST.matches(request.getMethod())
                || !"/perform-login".equals(request.getServletPath())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientKey = request.getRemoteAddr();
        long now = System.currentTimeMillis();
        AttemptWindow window = attempts.compute(clientKey, (key, current) -> {
            if (current == null || now - current.startedAt >= WINDOW_MILLIS) {
                return new AttemptWindow(now, 1);
            }
            current.count++;
            return current;
        });

        if (window.count > MAX_ATTEMPTS) {
            long retryAfterSeconds = Math.max(1,
                    (WINDOW_MILLIS - (now - window.startedAt) + 999) / 1000);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.sendError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    "Too many login attempts. Please try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static final class AttemptWindow {
        private final long startedAt;
        private int count;

        private AttemptWindow(long startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
