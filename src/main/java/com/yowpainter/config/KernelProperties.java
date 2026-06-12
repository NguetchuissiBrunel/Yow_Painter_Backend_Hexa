package com.yowpainter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ksm.kernel")
public record KernelProperties(
        String baseUrl,
        String clientId,
        String apiKey,
        String tenantId,
        String jwkSetUri,
        String defaultPlanCode,
        String defaultCurrency,
        String bootstrapAdminUsername,
        String bootstrapAdminPassword
) {
    public String resolvedJwkSetUri() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return jwkSetUri;
        }
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
        return base + "/.well-known/jwks.json";
    }
}
