package com.yowpainter.shared.kernel.port;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface KernelNotificationPort {

    void send(SendNotificationCommand command, String accessToken);

    List<DeliveryView> listDeliveries(UUID organizationId, String accessToken);

    record DeliveryView(
            UUID id,
            UUID recipientUserId,
            String subject,
            String body,
            String status,
            Instant requestedAt
    ) {
    }

    record SendNotificationCommand(
            UUID organizationId,
            UUID recipientUserId,
            String recipientAddress,
            String channel,
            String templateCode,
            String subject,
            String body,
            Map<String, Object> variables
    ) {
    }
}
