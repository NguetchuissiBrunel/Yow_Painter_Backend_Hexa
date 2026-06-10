package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelCreateSalesOrderRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelSalesOrderResponseDto;
import com.yowpainter.shared.kernel.port.KernelSalesPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KernelSalesHttpAdapter implements KernelSalesPort {

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;

    public KernelSalesHttpAdapter(KernelHttpClient kernelHttpClient, KernelProperties properties) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
    }

    @Override
    public SalesOrderView createOrder(CreateSalesOrderCommand command, String accessToken) {
        KernelSalesOrderResponseDto response = kernelHttpClient.post(
                "/api/sales/orders",
                new KernelCreateSalesOrderRequestDto(
                        command.organizationId(),
                        command.productId(),
                        command.quantity(),
                        command.unitPrice(),
                        command.currency() != null ? command.currency() : properties.defaultCurrency(),
                        command.orderNumber()
                ),
                KernelSalesOrderResponseDto.class,
                command.organizationId(),
                accessToken
        );
        return map(response);
    }

    @Override
    public SalesOrderView getOrder(UUID orderId, String accessToken) {
        return map(kernelHttpClient.get("/api/sales/orders/" + orderId, KernelSalesOrderResponseDto.class, null, accessToken));
    }

    @Override
    public SalesOrderView cancelOrder(UUID orderId, String accessToken) {
        return map(kernelHttpClient.post(
                "/api/sales/orders/" + orderId + "/cancel",
                java.util.Collections.emptyMap(),
                KernelSalesOrderResponseDto.class,
                null,
                accessToken
        ));
    }

    @Override
    public SalesOrderView confirmOrder(UUID orderId, String accessToken) {
        return map(kernelHttpClient.post(
                "/api/sales/orders/" + orderId + "/confirm",
                java.util.Collections.emptyMap(),
                KernelSalesOrderResponseDto.class,
                null,
                accessToken
        ));
    }

    @Override
    public List<SalesOrderView> listOrders(UUID organizationId, String accessToken) {
        return kernelHttpClient.getListWithQuery(
                "/api/sales/orders",
                Map.of("organizationId", organizationId.toString()),
                KernelSalesOrderResponseDto.class,
                organizationId,
                accessToken
        ).stream().map(this::map).toList();
    }

    private SalesOrderView map(KernelSalesOrderResponseDto dto) {
        return new SalesOrderView(
                dto.id(),
                dto.organizationId(),
                dto.productId(),
                dto.quantity(),
                dto.unitPrice(),
                dto.totalAmount(),
                dto.currency(),
                dto.status()
        );
    }
}
