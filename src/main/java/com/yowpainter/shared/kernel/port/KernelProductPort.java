package com.yowpainter.shared.kernel.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface KernelProductPort {

    ProductView createProduct(CreateProductCommand command, String accessToken);

    ProductView getProduct(UUID productId, String accessToken);

    List<ProductView> listProducts(UUID organizationId, String accessToken);

    record CreateProductCommand(
            UUID organizationId,
            String sku,
            String name,
            String description,
            BigDecimal unitPrice,
            String currency
    ) {
    }

    record ProductView(
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
}
