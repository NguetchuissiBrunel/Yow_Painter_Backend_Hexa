package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelSalesOrderResponseDto(
        UUID id,
        UUID organizationId,
        UUID productId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        String currency,
        String status
) {
}
