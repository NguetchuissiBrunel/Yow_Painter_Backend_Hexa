package com.yowpainter.shared.kernel.adapter.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record KernelCreateProductRequestDto(
        UUID organizationId,
        String sku,
        String name,
        String familyCode,
        String variantLabel,
        String description,
        BigDecimal unitPrice,
        String currency,
        String status
) {
}
