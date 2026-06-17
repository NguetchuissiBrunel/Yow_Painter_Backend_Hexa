package com.yowpainter.shared.kernel.port;

import java.util.UUID;

public interface KernelOrganizationPort {

    OrganizationView createOrganization(CreateOrganizationCommand command, String accessToken);

    java.util.Optional<java.util.UUID> findOrganizationIdByCode(String code, String accessToken);

    void approveOrganization(UUID organizationId, String reason, String adminAccessToken);

    void applyCommercialPlan(UUID organizationId, String planCode, String accessToken);

    record CreateOrganizationCommand(
            UUID businessActorId,
            String code,
            String shortName,
            String longName,
            String email
    ) {
    }

    record OrganizationView(UUID id, UUID businessActorId, String code, String shortName, String longName) {
    }
}
