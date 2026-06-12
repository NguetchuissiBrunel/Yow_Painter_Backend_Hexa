package com.yowpainter.shared.security;

import com.yowpainter.shared.context.RequestContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class KernelAccessTokenResolver {

    private KernelAccessTokenResolver() {
    }

    public static String requireAccessToken(Authentication authentication) {
        String token = resolveAccessToken(authentication);
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Token utilisateur requis. Reconnectez-vous via POST /api/auth/login."
            );
        }
        return token;
    }

    public static String resolveAccessToken(Authentication authentication) {
        String fromContext = RequestContext.accessToken();
        if (fromContext != null && !fromContext.isBlank()) {
            return fromContext;
        }
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            return jwtAuth.getToken().getTokenValue();
        }
        if (authentication != null && authentication.getCredentials() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return null;
    }
}
