package com.yowpainter.shared.kernel.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelBusinessEventMessage(
        UUID id,
        UUID tenantId,
        UUID organizationId,
        String eventType,
        String aggregateType,
        UUID aggregateId,
        Instant occurredAt,
        Map<String, Object> payload,
        String clientApplicationId
) {
}
