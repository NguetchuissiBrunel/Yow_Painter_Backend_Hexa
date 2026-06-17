package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelApplyCommercialPlanRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelCreateOrganizationRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelGovernanceActionRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelOrganizationResponseDto;
import com.yowpainter.shared.kernel.port.KernelOrganizationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class KernelOrganizationHttpAdapter implements KernelOrganizationPort {

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;

    public KernelOrganizationHttpAdapter(KernelHttpClient kernelHttpClient, KernelProperties properties) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
    }

    @Override
    public OrganizationView createOrganization(CreateOrganizationCommand command, String accessToken) {
        com.yowpainter.shared.kernel.JwtTokenParser.JwtTokenInfo info = 
                com.yowpainter.shared.kernel.JwtTokenParser.parseToken(accessToken);
        log.info("[JWT-AUDIT] Pre-request API: POST /api/organizations. actorId={}, userId={}, tenantId={}, roles={}, permissions={}, adm={}",
                info.actorId(), info.userId(), info.tenantId(), info.roles(), info.permissions(), info.adm());

        KernelOrganizationResponseDto response = kernelHttpClient.post(
                "/api/organizations",
                new KernelCreateOrganizationRequestDto(
                        command.businessActorId(),
                        command.code(),
                        "FREELANCE",
                        true,
                        command.email(),
                        command.shortName(),
                        command.longName()
                ),
                KernelOrganizationResponseDto.class,
                null,
                accessToken
        );
        return new OrganizationView(response.id(), response.businessActorId(), response.code(),
                response.shortName(), response.longName());
    }

    @Override
    public java.util.Optional<UUID> findOrganizationIdByCode(String code, String accessToken) {
        com.yowpainter.shared.kernel.JwtTokenParser.JwtTokenInfo info = 
                com.yowpainter.shared.kernel.JwtTokenParser.parseToken(accessToken);
        log.info("[JWT-AUDIT] Pre-request API: GET /api/organizations/search. actorId={}, userId={}, tenantId={}, roles={}, permissions={}, adm={}",
                info.actorId(), info.userId(), info.tenantId(), info.roles(), info.permissions(), info.adm());

        try {
            java.util.List<KernelOrganizationResponseDto> results = kernelHttpClient.getListWithQuery(
                    "/api/organizations/search",
                    java.util.Map.of("q", code),
                    KernelOrganizationResponseDto.class,
                    null,
                    accessToken
            );
            if (results != null) {
                return results.stream()
                        .filter(org -> code.equalsIgnoreCase(org.code()))
                        .map(KernelOrganizationResponseDto::id)
                        .findFirst();
            }
        } catch (Exception ex) {
            System.err.println("Failed to search organization by code on Kernel: " + ex.getMessage());
        }
        return java.util.Optional.empty();
    }

    @Override
    public void approveOrganization(UUID organizationId, String reason, String adminAccessToken) {
        kernelHttpClient.postVoid(
                "/api/organizations/" + organizationId + "/approve",
                new KernelGovernanceActionRequestDto(reason),
                organizationId,
                adminAccessToken
        );
    }

    @Override
    public void applyCommercialPlan(UUID organizationId, String planCode, String accessToken) {
        kernelHttpClient.post(
                "/api/organizations/" + organizationId + "/commercial-subscriptions",
                new KernelApplyCommercialPlanRequestDto(
                        planCode != null && !planCode.isBlank() ? planCode : properties.defaultPlanCode()
                ),
                Object.class,
                organizationId,
                accessToken
        );
    }
}
