package com.yowpainter.shared.kernel.event;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.modules.shop.domain.model.Order;
import com.yowpainter.modules.shop.domain.model.OrderStatus;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KernelBusinessEventHandler {

    private final KernelProperties kernelProperties;
    private final OrderRepositoryPort orderRepository;

    @Transactional
    public void handle(KernelBusinessEventMessage event) {
        if (event == null || event.eventType() == null) {
            return;
        }
        if (!isRelevantTenant(event.tenantId())) {
            return;
        }

        switch (event.eventType()) {
            case "SALES_ORDER_CONFIRMED" -> syncSalesOrder(event.aggregateId(), OrderStatus.PAID);
            case "SALES_ORDER_CANCELLED" -> syncSalesOrder(event.aggregateId(), OrderStatus.CANCELLED);
            default -> log.debug("Événement kernel ignoré: {}", event.eventType());
        }
    }

    private boolean isRelevantTenant(UUID eventTenantId) {
        if (kernelProperties.tenantId() == null || kernelProperties.tenantId().isBlank()) {
            return true;
        }
        try {
            return UUID.fromString(kernelProperties.tenantId()).equals(eventTenantId);
        } catch (IllegalArgumentException ex) {
            log.warn("KSM_KERNEL_TENANT_ID invalide: {}", kernelProperties.tenantId());
            return false;
        }
    }

    private void syncSalesOrder(UUID kernelSalesOrderId, OrderStatus targetStatus) {
        orderRepository.findByKernelSalesOrderId(kernelSalesOrderId).ifPresentOrElse(order -> {
            if (order.getStatus() == targetStatus) {
                return;
            }
            order.setStatus(targetStatus);
            orderRepository.save(order);
            log.info("Commande locale {} synchronisée -> {} (kernelOrder={})",
                    order.getId(), targetStatus, kernelSalesOrderId);
        }, () -> log.debug("Aucune commande locale pour kernelSalesOrderId={}", kernelSalesOrderId));
    }
}
