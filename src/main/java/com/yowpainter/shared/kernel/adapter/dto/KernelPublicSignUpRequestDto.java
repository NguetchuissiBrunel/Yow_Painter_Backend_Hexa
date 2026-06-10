package com.yowpainter.shared.kernel.adapter.dto;

import java.util.UUID;

public record KernelPublicSignUpRequestDto(
        UUID tenantId,
        String firstName,
        String lastName,
        String username,
        String email,
        String password,
        String accountType,
        String businessType
) {
}
