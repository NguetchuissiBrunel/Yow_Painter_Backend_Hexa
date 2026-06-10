package com.yowpainter.shared.kernel.adapter.dto;

import java.util.Map;
import java.util.UUID;

public record KernelSendNotificationRequestDto(
        UUID recipientUserId,
        String recipientAddress,
        String channel,
        String templateCode,
        String subject,
        String body,
        Map<String, Object> variables,
        Map<String, Object> metadata
) {
}
