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
        String bootstrapAdminPassword,
        String bootstrapClientId,
        String bootstrapApiKey,
        String signupPlatformOrganizationCode,
        boolean autoProvisionArtists,
        String systemUserEmail,
        String systemUserPassword
) {
    public String effectiveBootstrapClientId() {
        return bootstrapClientId == null || bootstrapClientId.isBlank() ? clientId() : bootstrapClientId;
    }

    public String effectiveBootstrapApiKey() {
        return bootstrapApiKey == null || bootstrapApiKey.isBlank() ? apiKey() : bootstrapApiKey;
    }

    public String resolvedJwkSetUri() {
        if (jwkSetUri != null && !jwkSetUri.isBlank()) {
            return jwkSetUri;
        }
        String base = baseUrl == null ? "" : baseUrl.replaceAll("/$", "");
        return base + "/.well-known/jwks.json";
    }
}
