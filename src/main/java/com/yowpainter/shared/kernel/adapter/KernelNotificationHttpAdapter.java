package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelNotificationDeliveryDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelSendNotificationRequestDto;
import com.yowpainter.shared.kernel.port.KernelNotificationPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KernelNotificationHttpAdapter implements KernelNotificationPort {

    private final KernelHttpClient kernelHttpClient;

    public KernelNotificationHttpAdapter(KernelHttpClient kernelHttpClient) {
        this.kernelHttpClient = kernelHttpClient;
    }

    @Override
    public void send(SendNotificationCommand command, String accessToken) {
        kernelHttpClient.post(
                "/api/notifications/deliveries",
                new KernelSendNotificationRequestDto(
                        command.recipientUserId(),
                        command.recipientAddress(),
                        command.channel() != null ? command.channel() : "WEBSOCKET",
                        command.templateCode(),
                        command.subject(),
                        command.body(),
                        command.variables(),
                        Map.of("source", "yowpainter-backend")
                ),
                Object.class,
                command.organizationId(),
                accessToken
        );
    }

    @Override
    public List<DeliveryView> listDeliveries(UUID organizationId, String accessToken) {
        return kernelHttpClient.getRawList(
                "/api/notifications/deliveries",
                KernelNotificationDeliveryDto.class,
                organizationId,
                accessToken
        ).stream()
                .map(delivery -> new DeliveryView(
                        delivery.id(),
                        delivery.recipientUserId(),
                        delivery.subject(),
                        delivery.body(),
                        delivery.status(),
                        delivery.requestedAt()
                ))
                .toList();
    }
}
