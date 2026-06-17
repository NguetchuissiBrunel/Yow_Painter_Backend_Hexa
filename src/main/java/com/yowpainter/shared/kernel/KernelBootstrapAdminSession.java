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

    public String getBootstrapAdminAccessToken() {
        return requireAccessToken();
    }

    public String requireAccessToken() {
        return requireAccessToken(null);
    }

    public String requireAccessToken(String mfaCode) {
        if (mfaCode != null && !mfaCode.isBlank()) {
            invalidate();
        }

        // 1. Si un token bootstrap/technique valide est déjà en cache, l'utiliser en priorité absolue !
        CachedToken current = cachedToken;
        if (current != null && current.isValid()) {
            return current.accessToken();
        }

        // 2. Utiliser le cache synchronisé ou tenter le login bootstrap
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

    public void confirmMfaLogin(String mfaToken, String code) {
        log.info("[provision] MFA_CONFIRM avec le token: {}...", mfaToken);
        try {
            KernelAuthLoginPayloadDto confirmed = kernelHttpClient.postBootstrap(
                    "/api/auth/login/mfa/confirm",
                    new KernelConfirmMfaLoginRequestDto(mfaToken, code),
                    KernelAuthLoginPayloadDto.class
            );
            if (confirmed.accessToken() == null || confirmed.accessToken().isBlank()) {
                throw new IllegalStateException("MFA_FAILED : Le token d'accès n'a pas pu être récupéré après confirmation MFA.");
            }
            synchronized (this) {
                cachedToken = CachedToken.of(confirmed.accessToken(), confirmed.expiresInSeconds());
            }
            log.info("[provision] MFA_SUCCESS. Token administrateur mis en cache.");
        } catch (KernelClientException ex) {
            log.error("[provision] MFA_FAILED : {}", ex.getMessage());
            throw ex;
        }
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

        // Si MFA requis, lever l'exception avec le token MFA
        if (login.mfaToken() != null && !login.mfaToken().isBlank()) {
            log.info("[provision] MFA_REQUIRED détecté (mfaToken={})", login.mfaToken());
            if (mfaCode != null && !mfaCode.isBlank()) {
                // Tenter de confirmer avec le code fourni
                try {
                    KernelAuthLoginPayloadDto confirmed = kernelHttpClient.postBootstrap(
                            "/api/auth/login/mfa/confirm",
                            new KernelConfirmMfaLoginRequestDto(login.mfaToken(), mfaCode),
                            KernelAuthLoginPayloadDto.class
                    );
                    if (confirmed.accessToken() != null && !confirmed.accessToken().isBlank()) {
                        return CachedToken.of(confirmed.accessToken(), confirmed.expiresInSeconds());
                    }
                } catch (KernelClientException ex) {
                    log.error("[provision] Erreur lors de la confirmation MFA : {}", ex.getMessage());
                    throw ex;
                }
            } else if (login.codePreview() != null && !login.codePreview().isBlank()) {
                // Mode developpement/sandbox avec codePreview (si disponible)
                try {
                    KernelAuthLoginPayloadDto confirmed = kernelHttpClient.postBootstrap(
                            "/api/auth/login/mfa/confirm",
                            new KernelConfirmMfaLoginRequestDto(login.mfaToken(), login.codePreview()),
                            KernelAuthLoginPayloadDto.class
                    );
                    if (confirmed.accessToken() != null && !confirmed.accessToken().isBlank()) {
                        return CachedToken.of(confirmed.accessToken(), confirmed.expiresInSeconds());
                    }
                } catch (Exception ex) {
                    log.debug("Confirmation via codePreview echouee, envoi du challenge MFA requis: {}", ex.getMessage());
                }
            }
            throw new KernelMfaRequiredException(login.mfaToken());
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

    public record TokenDetails(
        java.util.List<String> roles,
        java.util.List<String> permissions,
        boolean adm
    ) {}

    public TokenDetails parseTokenDetails(String token) {
        JwtTokenParser.JwtTokenInfo info = JwtTokenParser.parseToken(token);
        return new TokenDetails(info.roles(), info.permissions(), info.adm());
    }

    public void inspectAndVerifyToken(String token, String requiredPermission) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid JWT token format");
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(payload);
            
            // Masked token for logging
            String maskedToken = token.substring(0, Math.min(token.length(), 20)) + "..." + token.substring(Math.max(0, token.length() - 20));
            log.info("[JWT-AUDIT] Token utilise (masque): {}", maskedToken);
            log.info("[JWT-AUDIT] Token utilise (complet): {}", token);
            log.info("[JWT-AUDIT] Claims JWT de-codes: {}", node.toString());
            
            TokenDetails details = parseTokenDetails(token);
            log.info("[JWT-AUDIT] Valeur du claim \"adm\": {}", details.adm());
            log.info("[JWT-AUDIT] Roles contenus dans le token: {}", details.roles());
            log.info("[JWT-AUDIT] Permissions contenues dans le token: {}", details.permissions());
            
            // Verification of roles/permissions according to required spec
            boolean hasRolePlatformAdmin = details.roles().contains("ROLE_PLATFORM_ADMIN");
            boolean hasRoleSuperAdmin = details.roles().contains("ROLE_SUPER_ADMIN");
            boolean hasOrgCreate = details.permissions().contains("ORGANIZATION_CREATE");
            boolean hasOrgWrite = details.permissions().contains("ORGANIZATION_WRITE") || details.permissions().contains("organizations:write");
            
            log.info("[JWT-AUDIT] Verification des privileges attendus : ROLE_PLATFORM_ADMIN={}, ROLE_SUPER_ADMIN={}, ORGANIZATION_CREATE={}, ORGANIZATION_WRITE={}",
                    hasRolePlatformAdmin, hasRoleSuperAdmin, hasOrgCreate, hasOrgWrite);
            
            // Combine all roles & permissions to verify the specific requiredPermission
            java.util.List<String> allPermissions = new java.util.ArrayList<>();
            allPermissions.addAll(details.roles());
            allPermissions.addAll(details.permissions());
            
            boolean hasPermission = false;
            for (String perm : allPermissions) {
                String normalized = perm.split("#")[0]; // remove scoped suffix
                if (normalized.equals(requiredPermission)) {
                    hasPermission = true;
                    break;
                }
            }
            
            if (!hasPermission) {
                log.error("[JWT-AUDIT] Echec verification: Permission manquante: {}", requiredPermission);
                throw new KernelPermissionDeniedException(requiredPermission);
            }
            log.info("[JWT-AUDIT] Verification de la permission '{}' reussie.", requiredPermission);
            
        } catch (KernelPermissionDeniedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[JWT-AUDIT] Erreur lors du de-codage et de la verification du token admin: {}", ex.getMessage());
            throw new IllegalStateException("Impossible de valider les permissions du token administrateur", ex);
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
