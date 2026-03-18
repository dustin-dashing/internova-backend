package com.internova.integration.brain.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class WebhookSecurityFilter extends OncePerRequestFilter {

    private final String webhookSecret;

    public WebhookSecurityFilter(@Value("${internova.webhook.secret}") String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/api/v1/webhooks/")) {
            String providedSecret = request.getHeader("X-Webhook-Secret");

            if (providedSecret == null || !isValidSecret(providedSecret)) {
                logger.warn("Unauthorized webhook access attempt to " + path);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Unauthorized Webhook Call");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidSecret(String providedSecret) {
        return MessageDigest.isEqual(
                providedSecret.getBytes(StandardCharsets.UTF_8),
                webhookSecret.getBytes(StandardCharsets.UTF_8)
        );
    }
}