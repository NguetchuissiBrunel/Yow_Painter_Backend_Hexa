package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelProductResponseDto(
        UUID id,
        UUID organizationId,
        String sku,
        String name,
        String description,
        BigDecimal unitPrice,
        String currency,
        String status
) {
}
