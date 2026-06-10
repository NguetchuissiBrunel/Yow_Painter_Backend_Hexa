package com.yowpainter.shared.kernel.adapter.dto;

import java.util.UUID;

public record KernelAssignAdministrativeRoleRequestDto(
        UUID roleId,
        String scopeType,
        UUID scopeId,
        String scope
) {
}
