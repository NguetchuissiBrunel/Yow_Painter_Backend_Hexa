package com.yowpainter.shared.kernel.adapter.dto;

import java.util.UUID;

public record KernelCreateOrganizationRequestDto(
        UUID businessActorId,
        String code,
        String service,
        boolean isIndividualBusiness,
        String email,
        String shortName,
        String longName
) {
}
