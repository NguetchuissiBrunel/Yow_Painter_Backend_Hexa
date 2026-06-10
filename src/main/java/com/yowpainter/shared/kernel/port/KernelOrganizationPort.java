package com.yowpainter.shared.kernel.port;

import java.util.UUID;

public interface KernelOrganizationPort {

    OrganizationView createOrganization(CreateOrganizationCommand command, String accessToken);

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
