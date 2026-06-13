package com.yowpainter.shared.kernel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowpainter.config.KernelProperties;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelAuthLoginPayloadDto;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelConfirmMfaEnableRequestDto;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelConfirmMfaLoginRequestDto;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelEnableMfaRequestDto;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelLoginRequestDto;
import com.yowpainter.modules.auth.infrastructure.adapter.out.kernel.dto.KernelOtpChallengePayloadDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Base64;

@Component
@Slf4j
public class KernelBootstrapAdminSession {

    private static final String ORGANIZATION_ADMIN_ROLE = "ORGANIZATION_ADMIN";
    private static final long TOKEN_REFRESH_MARGIN_SECONDS = 60;

    private static final String JWT_CLAIM_MFA_ENABLED = "mfa";
    private static final String JWT_CLAIM_PRIVILEGED_ADMIN = "adm";

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;
    private final ObjectMapper objectMapper;

    private volatile CachedToken cachedToken;

    public KernelBootstrapAdminSession(
            KernelHttpClient kernelHttpClient,
            KernelProperties properties,
            ObjectMapper objectMapper
    ) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void invalidate() {
        cachedToken = null;
    }

    public String requireAccessToken() {
        return requireAccessToken(null);
    }

    public String requireAccessToken(String mfaCode) {
        if (mfaCode != null && !mfaCode.isBlank()) {
            invalidate();
        }

        CachedToken current = cachedToken;
        if (current != null && current.isValid()) {
            return current.accessToken();
        }

        synchronized (this) {
            current = cachedToken;
            if (current != null && current.isValid()) {
                return current.accessToken();
            }
            CachedToken refreshed = loginBootstrapAdmin(mfaCode);
            cachedToken = refreshed;
            return refreshed.accessToken();
        }
    }

    private CachedToken loginBootstrapAdmin() {
        return loginBootstrapAdmin(null);
    }

    private CachedToken loginBootstrapAdmin(String mfaCode) {
        String username = properties.bootstrapAdminUsername();
        String password = properties.bootstrapAdminPassword();
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "KSM_KERNEL_BOOTSTRAP_ADMIN_USERNAME/PASSWORD requis pour provisionner un artiste (role "
                            + ORGANIZATION_ADMIN_ROLE + ")."
            );
        }

        CachedToken session = loginWithOptionalMfa(username, password, mfaCode);
        if (isPrivilegedAdminToken(session.accessToken()) && !isMfaEnabledToken(session.accessToken())) {
            enableAccountMfa(session.accessToken(), username);
            session = loginWithOptionalMfa(username, password, mfaCode);
        }
        if (isPrivilegedAdminToken(session.accessToken()) && !isMfaEnabledToken(session.accessToken())) {
            throw new IllegalStateException(
                    "Le compte bootstrap " + username
                            + " doit avoir le MFA active dans le kernel (JWT claim mfa=true apres connexion)."
            );
        }
        return session;
    }

    private CachedToken loginWithOptionalMfa(String username, String password, String mfaCode) {
        KernelAuthLoginPayloadDto login = kernelHttpClient.postBootstrap(
                "/api/auth/login",
                new KernelLoginRequestDto(username, password),
                KernelAuthLoginPayloadDto.class
        );

        if (login.accessToken() != null && !login.accessToken().isBlank()) {
            return CachedToken.of(login.accessToken(), login.expiresInSeconds());
        }

        if (login.mfaToken() != null && login.codePreview() != null) {
            KernelAuthLoginPayloadDto confirmed = kernelHttpClient.postBootstrap(
                    "/api/auth/login/mfa/confirm",
                    new KernelConfirmMfaLoginRequestDto(login.mfaToken(), login.codePreview()),
                    KernelAuthLoginPayloadDto.class
            );
            if (confirmed.accessToken() != null && !confirmed.accessToken().isBlank()) {
                return CachedToken.of(confirmed.accessToken(), confirmed.expiresInSeconds());
            }
        }

        if (login.mfaToken() != null && mfaCode != null && !mfaCode.isBlank()) {
            KernelAuthLoginPayloadDto confirmed = kernelHttpClient.postBootstrap(
                    "/api/auth/login/mfa/confirm",
                    new KernelConfirmMfaLoginRequestDto(login.mfaToken(), mfaCode),
                    KernelAuthLoginPayloadDto.class
            );
            if (confirmed.accessToken() != null && !confirmed.accessToken().isBlank()) {
                return CachedToken.of(confirmed.accessToken(), confirmed.expiresInSeconds());
            }
        }

        throw new IllegalStateException(
                "Connexion bootstrap admin kernel impossible (MFA requis ou credentials invalides)."
        );
    }

    private void enableAccountMfa(String accessToken, String username) {
        try {
            KernelOtpChallengePayloadDto challenge = kernelHttpClient.postBootstrap(
                    "/api/auth/mfa/enable",
                    new KernelEnableMfaRequestDto("EMAIL"),
                    KernelOtpChallengePayloadDto.class,
                    accessToken
            );
            if (challenge.challengeToken() == null || challenge.codePreview() == null) {
                throw new IllegalStateException("Reponse MFA enable bootstrap invalide (challenge manquant).");
            }
            kernelHttpClient.postBootstrap(
                    "/api/auth/mfa/confirm",
                    new KernelConfirmMfaEnableRequestDto(challenge.challengeToken(), challenge.codePreview()),
                    Object.class,
                    accessToken
            );
            log.info("MFA active sur le compte bootstrap kernel {}", username);
        } catch (KernelClientException ex) {
            if (isMfaAlreadyEnabledError(ex)) {
                log.debug("MFA deja active sur bootstrap admin {}: {}", username, ex.getMessage());
                return;
            }
            throw new IllegalStateException(
                    "Impossible d'activer le MFA bootstrap admin " + username + ": " + ex.getMessage(),
                    ex
            );
        }
    }

    private boolean isMfaAlreadyEnabledError(KernelClientException ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return message.contains("already enabled") || message.contains("deja active");
    }

    private boolean isPrivilegedAdminToken(String accessToken) {
        return readJwtBooleanClaim(accessToken, JWT_CLAIM_PRIVILEGED_ADMIN);
    }

    private boolean isMfaEnabledToken(String accessToken) {
        return readJwtBooleanClaim(accessToken, JWT_CLAIM_MFA_ENABLED);
    }

    private boolean readJwtBooleanClaim(String accessToken, String claimName) {
        if (accessToken == null || accessToken.isBlank()) {
            return false;
        }
        String[] parts = accessToken.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(payload);
            JsonNode claim = node.get(claimName);
            return claim != null && claim.asBoolean(false);
        } catch (Exception ex) {
            log.warn("Impossible de lire le claim JWT {}: {}", claimName, ex.getMessage());
            return false;
        }
    }

    private record CachedToken(String accessToken, Instant expiresAt) {
        static CachedToken of(String accessToken, long expiresInSeconds) {
            long ttl = expiresInSeconds > 0 ? expiresInSeconds : 900;
            return new CachedToken(accessToken, Instant.now().plusSeconds(ttl));
        }

        boolean isValid() {
            return accessToken != null
                    && !accessToken.isBlank()
                    && Instant.now().isBefore(expiresAt.minusSeconds(TOKEN_REFRESH_MARGIN_SECONDS));
        }
    }
}
