package com.yowpainter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ksm.kernel.kafka")
public record KernelKafkaProperties(
        boolean enabled,
        String bootstrapServers,
        String topic,
        String groupId
) {
    public String resolvedTopic() {
        return topic == null || topic.isBlank() ? "iwm.events.business" : topic;
    }

    public String resolvedGroupId() {
        return groupId == null || groupId.isBlank() ? "yowpainter-backend" : groupId;
    }
}
