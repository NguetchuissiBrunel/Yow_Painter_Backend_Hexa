package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Set;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelAdministrativeRoleResponseDto(
        UUID id,
        UUID tenantId,
        String code,
        String name,
        String scopeType,
        Set<String> permissions
) {
}
