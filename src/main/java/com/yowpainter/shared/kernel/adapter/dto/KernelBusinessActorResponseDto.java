package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelBusinessActorResponseDto(
        UUID id,
        UUID actorId,
        String code,
        String name
) {
}
