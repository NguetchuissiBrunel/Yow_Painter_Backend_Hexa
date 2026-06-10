package com.yowpainter.shared.kernel.event;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.modules.shop.domain.model.Order;
import com.yowpainter.modules.shop.domain.model.OrderStatus;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KernelBusinessEventHandlerTest {

    @Mock
    private KernelProperties kernelProperties;

    @Mock
    private OrderRepositoryPort orderRepository;

    @InjectMocks
    private KernelBusinessEventHandler handler;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID kernelOrderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(kernelProperties.tenantId()).thenReturn(tenantId.toString());
    }

    @Test
    void shouldSyncOrderToPaidOnSalesOrderConfirmed() {
        Order order = Order.builder().status(OrderStatus.PENDING_PAYMENT).build();
        order.setId(UUID.randomUUID());

        when(orderRepository.findByKernelSalesOrderId(kernelOrderId)).thenReturn(Optional.of(order));

        handler.handle(new KernelBusinessEventMessage(
                UUID.randomUUID(),
                tenantId,
                UUID.randomUUID(),
                "SALES_ORDER_CONFIRMED",
                "SALES_ORDER",
                kernelOrderId,
                null,
                null,
                null
        ));

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.PAID));
    }

    @Test
    void shouldIgnoreEventsFromOtherTenants() {
        when(kernelProperties.tenantId()).thenReturn(UUID.randomUUID().toString());

        handler.handle(new KernelBusinessEventMessage(
                UUID.randomUUID(),
                tenantId,
                null,
                "SALES_ORDER_CONFIRMED",
                "SALES_ORDER",
                kernelOrderId,
                null,
                null,
                null
        ));

        verify(orderRepository, never()).findByKernelSalesOrderId(any());
    }
}
