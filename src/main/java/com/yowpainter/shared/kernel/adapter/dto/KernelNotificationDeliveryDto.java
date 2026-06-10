package com.yowpainter.shared.kernel.adapter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelNotificationDeliveryDto(
        UUID id,
        UUID tenantId,
        UUID organizationId,
        UUID recipientUserId,
        String recipientAddress,
        String channel,
        String templateCode,
        String subject,
        String body,
        Map<String, Object> variables,
        Map<String, String> metadata,
        String status,
        Instant requestedAt,
        Instant sentAt
) {
}
