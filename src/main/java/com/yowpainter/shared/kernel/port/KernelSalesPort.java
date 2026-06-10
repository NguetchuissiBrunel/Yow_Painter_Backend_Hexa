package com.yowpainter.shared.kernel.port;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface KernelSalesPort {

    SalesOrderView createOrder(CreateSalesOrderCommand command, String accessToken);

    SalesOrderView getOrder(UUID orderId, String accessToken);

    SalesOrderView cancelOrder(UUID orderId, String accessToken);

    SalesOrderView confirmOrder(UUID orderId, String accessToken);

    List<SalesOrderView> listOrders(UUID organizationId, String accessToken);

    record CreateSalesOrderCommand(
            UUID organizationId,
            UUID productId,
            BigDecimal quantity,
            BigDecimal unitPrice,
            String currency,
            String orderNumber
    ) {
    }

    record SalesOrderView(
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
}
