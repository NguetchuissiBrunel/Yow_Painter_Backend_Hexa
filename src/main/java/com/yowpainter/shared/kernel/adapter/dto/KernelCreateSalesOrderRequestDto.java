package com.yowpainter.shared.kernel.adapter.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record KernelCreateSalesOrderRequestDto(
        UUID organizationId,
        UUID productId,
        BigDecimal quantity,
        BigDecimal unitPrice,
        String currency,
        String orderNumber
) {
}
