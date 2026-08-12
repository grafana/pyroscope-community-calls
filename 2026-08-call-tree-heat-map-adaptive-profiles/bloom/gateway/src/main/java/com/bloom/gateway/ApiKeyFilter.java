package com.bloom.gateway;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    @Value("${bloom.auth.api-key-hash}")
    private String expectedHash;

    @Value("${bloom.auth.salt}")
    private String salt;

    @Value("${bloom.auth.iterations}")
    private int iterations;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Only the shop API is key-protected; the storefront assets are public.
        if (!request.getRequestURI().startsWith("/shop")) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-Api-Key");
        if (apiKey == null || !hash(apiKey).equals(expectedHash)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "invalid API key");
            return;
        }

        chain.doFilter(request, response);
    }

    // PBKDF2 with a healthy iteration count, as recommended by OWASP.
    private String hash(String apiKey) {
        PBEKeySpec spec = new PBEKeySpec(apiKey.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, 256);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return HexFormat.of().formatHex(factory.generateSecret(spec).getEncoded());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("failed to hash API key", e);
        }
    }
}
