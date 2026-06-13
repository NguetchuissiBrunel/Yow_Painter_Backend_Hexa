package com.yowpainter.shared.kernel.adapter.dto;

public record KernelBusinessActorOnboardingRequestDto(
        String code,
        String name,
        String businessId,
        String type,
        String role,
        boolean isIndividual,
        boolean isAvailable,
        boolean isVerified,
        boolean isActive
) {
}
