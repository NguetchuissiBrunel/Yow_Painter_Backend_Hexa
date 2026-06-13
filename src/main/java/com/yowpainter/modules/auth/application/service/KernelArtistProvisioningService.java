package com.yowpainter.modules.auth.application.service;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.shared.kernel.KernelBootstrapAdminSession;
import com.yowpainter.shared.kernel.KernelClientException;
import com.yowpainter.shared.kernel.port.KernelActorPort;
import com.yowpainter.shared.kernel.port.KernelAdministrationPort;
import com.yowpainter.shared.kernel.port.KernelOrganizationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
@Slf4j
public class KernelArtistProvisioningService {

    private static final String AUTO_APPROVAL_REASON = "Validation automatique YowPainter";
    private static final String MANUAL_APPROVAL_REASON = "Validation manuelle YowPainter";

    private final KernelActorPort kernelActorPort;
    private final KernelOrganizationPort kernelOrganizationPort;
    private final KernelAdministrationPort kernelAdministrationPort;
    private final KernelBootstrapAdminSession bootstrapAdminSession;
    private final KernelProperties kernelProperties;

    public ProvisioningResult provisionAfterOnboarding(Artist artist, KernelAuthPort.KernelLoginResult confirmed) {
        String artistEmail = artist.getEmail();
        log.info("[provision] Demarrage provisioning artiste {}", artistEmail);

        String userToken = runStep("VALIDATE_USER_TOKEN", artistEmail,
                () -> requireUserToken(confirmed));
        UUID userId = runStep("VALIDATE_USER_ID", artistEmail,
                () -> requireUserId(artist, confirmed));

        UUID businessActorId = resolveBusinessActorId(artist, confirmed, userToken);

        String adminToken = runStep("BOOTSTRAP_ADMIN_LOGIN", artistEmail,
                bootstrapAdminSession::requireAccessToken);

        UUID organizationId = resolveOrganizationId(artist, confirmed, businessActorId, adminToken);

        runStepVoid("ORGANIZATION_APPROVE (POST /api/organizations/{id}/approve)", artistEmail, () ->
                kernelOrganizationPort.approveOrganization(organizationId, AUTO_APPROVAL_REASON, adminToken)
        );
        runStepVoid(
                "COMMERCIAL_PLAN (POST /api/organizations/{id}/commercial-subscriptions, plan="
                        + kernelProperties.defaultPlanCode() + ")",
                artistEmail,
                () -> kernelOrganizationPort.applyCommercialPlan(
                        organizationId,
                        kernelProperties.defaultPlanCode(),
                        adminToken
                )
        );
        runStepVoid("ORGANIZATION_ADMIN_ROLE (POST /api/administration/users/{id}/roles)", artistEmail, () ->
                kernelAdministrationPort.grantOrganizationAdminRole(userId, organizationId)
        );

        log.info(
                "[provision] Termine avec succes pour {} (org={}, actor={}, userId={})",
                artistEmail,
                organizationId,
                businessActorId,
                userId
        );

        return new ProvisioningResult(organizationId, businessActorId, confirmed.tenantId());
    }

    public ProvisioningResult provisionOnAdminApproval(
            Artist artist,
            String bootstrapMfaCode,
            UUID kernelActorIdOverride
    ) {
        String artistEmail = artist.getEmail();
        log.info("[provision] Demarrage provisioning admin pour {}", artistEmail);

        UUID userId = runStep("VALIDATE_USER_ID", artistEmail,
                () -> requireUserIdFromArtist(artist));

        UUID businessActorId = runStep("VALIDATE_ACTOR_ID", artistEmail,
                () -> resolveActorIdForAdminApproval(artist, kernelActorIdOverride));

        String adminToken = runStep("BOOTSTRAP_ADMIN_LOGIN", artistEmail,
                () -> bootstrapAdminSession.requireAccessToken(bootstrapMfaCode));

        UUID organizationId = resolveOrganizationIdForAdminApproval(artist, businessActorId, adminToken);

        runStepVoid("ORGANIZATION_APPROVE (POST /api/organizations/{id}/approve)", artistEmail, () ->
                kernelOrganizationPort.approveOrganization(organizationId, MANUAL_APPROVAL_REASON, adminToken)
        );
        runStepVoid(
                "COMMERCIAL_PLAN (POST /api/organizations/{id}/commercial-subscriptions, plan="
                        + kernelProperties.defaultPlanCode() + ")",
                artistEmail,
                () -> kernelOrganizationPort.applyCommercialPlan(
                        organizationId,
                        kernelProperties.defaultPlanCode(),
                        adminToken
                )
        );
        runStepVoid("ORGANIZATION_ADMIN_ROLE (POST /api/administration/users/{id}/roles)", artistEmail, () ->
                kernelAdministrationPort.grantOrganizationAdminRole(userId, organizationId)
        );

        log.info(
                "[provision] Approbation admin terminee pour {} (org={}, actor={}, userId={})",
                artistEmail,
                organizationId,
                businessActorId,
                userId
        );

        return new ProvisioningResult(organizationId, businessActorId, artist.getTenantId());
    }

    private UUID resolveOrganizationIdForAdminApproval(
            Artist artist,
            UUID businessActorId,
            String adminToken
    ) {
        if (artist.getOrganizationId() != null) {
            log.info(
                    "[provision] ORGANIZATION_CREATE ignore — organizationId deja present ({}) pour {}",
                    artist.getOrganizationId(),
                    artist.getEmail()
            );
            return artist.getOrganizationId();
        }

        return runStep(
                "ORGANIZATION_CREATE (POST /api/organizations, code=" + artist.getSlug()
                        + ", token=bootstrap-admin)",
                artist.getEmail(),
                () -> {
                    KernelOrganizationPort.OrganizationView organization = kernelOrganizationPort.createOrganization(
                            new KernelOrganizationPort.CreateOrganizationCommand(
                                    businessActorId,
                                    artist.getSlug(),
                                    artist.getArtistName(),
                                    artist.getArtistName(),
                                    artist.getEmail()
                            ),
                            adminToken
                    );
                    log.info("[provision] ORGANIZATION_CREATE — organizationId={}", organization.id());
                    return organization.id();
                }
        );
    }

    private UUID resolveActorIdForAdminApproval(Artist artist, UUID kernelActorIdOverride) {
        if (kernelActorIdOverride != null) {
            return kernelActorIdOverride;
        }
        if (artist.getKernelActorId() != null) {
            return artist.getKernelActorId();
        }
        throw new KernelClientException(
                "Identifiant acteur kernel introuvable pour " + artist.getEmail()
                        + ". Renseignez kernelActorId dans la requete d'approbation.",
                null,
                null
        );
    }

    private UUID requireUserIdFromArtist(Artist artist) {
        if (artist.getKernelUserId() == null) {
            throw new KernelClientException(
                    "Identifiant utilisateur kernel introuvable pour " + artist.getEmail(),
                    null,
                    null
            );
        }
        return artist.getKernelUserId();
    }

    private UUID resolveBusinessActorId(
            Artist artist,
            KernelAuthPort.KernelLoginResult confirmed,
            String userToken
    ) {
        if (confirmed.actorId() != null) {
            log.info(
                    "[provision] ACTOR_ONBOARDING ignore — actorId deja present ({}) pour {}",
                    confirmed.actorId(),
                    artist.getEmail()
            );
            return confirmed.actorId();
        }

        return runStep(
                "ACTOR_ONBOARDING (POST /api/actors/onboarding)",
                artist.getEmail(),
                () -> {
                    KernelActorPort.BusinessActorView actor = kernelActorPort.submitOnboarding(
                            new KernelActorPort.OnboardingCommand(
                                    artist.getSlug(),
                                    artist.getArtistName(),
                                    artist.getSlug(),
                                    artist.getEmail()
                            ),
                            userToken
                    );
                    UUID actorId = actor.actorId() != null ? actor.actorId() : actor.id();
                    log.info("[provision] ACTOR_ONBOARDING — actorId={} businessActorId={}",
                            actor.actorId(), actor.id());
                    return actorId;
                }
        );
    }

    private UUID resolveOrganizationId(
            Artist artist,
            KernelAuthPort.KernelLoginResult confirmed,
            UUID businessActorId,
            String adminToken
    ) {
        if (artist.getOrganizationId() != null) {
            log.info(
                    "[provision] ORGANIZATION_CREATE ignore — organizationId deja present ({}) pour {}",
                    artist.getOrganizationId(),
                    artist.getEmail()
            );
            return artist.getOrganizationId();
        }
        if (confirmed.organizations() != null && !confirmed.organizations().isEmpty()) {
            UUID organizationId = confirmed.organizations().get(0).organizationId();
            log.info(
                    "[provision] ORGANIZATION_CREATE ignore — org presente dans JWT ({}) pour {}",
                    organizationId,
                    artist.getEmail()
            );
            return organizationId;
        }

        return runStep(
                "ORGANIZATION_CREATE (POST /api/organizations, code=" + artist.getSlug()
                        + ", token=bootstrap-admin)",
                artist.getEmail(),
                () -> {
                    KernelOrganizationPort.OrganizationView organization = kernelOrganizationPort.createOrganization(
                            new KernelOrganizationPort.CreateOrganizationCommand(
                                    businessActorId,
                                    artist.getSlug(),
                                    artist.getArtistName(),
                                    artist.getArtistName(),
                                    artist.getEmail()
                            ),
                            adminToken
                    );
                    log.info("[provision] ORGANIZATION_CREATE — organizationId={}", organization.id());
                    return organization.id();
                }
        );
    }

    private String requireUserToken(KernelAuthPort.KernelLoginResult confirmed) {
        if (confirmed.accessToken() == null || confirmed.accessToken().isBlank()) {
            throw new KernelClientException(
                    "Token utilisateur kernel manquant apres confirmation e-mail (reconnectez-vous puis reessayez).",
                    null,
                    null
            );
        }
        return confirmed.accessToken();
    }

    private UUID requireUserId(Artist artist, KernelAuthPort.KernelLoginResult confirmed) {
        UUID userId = confirmed.userId() != null ? confirmed.userId() : artist.getKernelUserId();
        if (userId == null) {
            throw new KernelClientException(
                    "Identifiant utilisateur kernel introuvable pour " + artist.getEmail(),
                    null,
                    null
            );
        }
        return userId;
    }

    private <T> T runStep(String step, String artistEmail, Supplier<T> action) {
        log.info("[provision] {} — demarrage (artiste={})", step, artistEmail);
        try {
            T result = action.get();
            log.info("[provision] {} — OK (artiste={})", step, artistEmail);
            return result;
        } catch (RuntimeException ex) {
            logProvisioningFailure(step, artistEmail, ex);
            throw ex;
        }
    }

    private void runStepVoid(String step, String artistEmail, Runnable action) {
        runStep(step, artistEmail, () -> {
            action.run();
            return null;
        });
    }

    private void logProvisioningFailure(String step, String artistEmail, RuntimeException ex) {
        if (ex instanceof KernelClientException kernelEx) {
            Integer httpStatus = kernelEx.statusCode() != null ? kernelEx.statusCode().value() : null;
            log.warn(
                    "[provision] {} — ECHEC (artiste={}, httpStatus={}, errorCode={}, message={})",
                    step,
                    artistEmail,
                    httpStatus != null ? httpStatus : "n/a",
                    kernelEx.errorCode() != null ? kernelEx.errorCode() : "n/a",
                    kernelEx.getMessage()
            );
            return;
        }
        log.warn(
                "[provision] {} — ECHEC (artiste={}, exception={}, message={})",
                step,
                artistEmail,
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
    }

    public record ProvisioningResult(UUID organizationId, UUID businessActorId, UUID tenantId) {
    }
}
