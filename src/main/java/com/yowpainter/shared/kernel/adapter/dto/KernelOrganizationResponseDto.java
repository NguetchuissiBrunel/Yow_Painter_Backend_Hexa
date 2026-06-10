package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelOrganizationResponseDto(
        UUID id,
        UUID businessActorId,
        String code,
        String shortName,
        String longName
) {
}
