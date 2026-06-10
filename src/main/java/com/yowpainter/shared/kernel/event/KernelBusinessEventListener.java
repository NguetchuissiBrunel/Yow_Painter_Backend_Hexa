package com.yowpainter.shared.kernel.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "ksm.kernel.kafka.enabled", havingValue = "true")
public class KernelBusinessEventListener {

    private final ObjectMapper objectMapper;
    private final KernelBusinessEventHandler eventHandler;

    @KafkaListener(
            topics = "${ksm.kernel.kafka.topic:iwm.events.business}",
            groupId = "${ksm.kernel.kafka.group-id:yowpainter-backend}"
    )
    public void onBusinessEvent(String payload) {
        try {
            KernelBusinessEventMessage event = objectMapper.readValue(payload, KernelBusinessEventMessage.class);
            eventHandler.handle(event);
        } catch (Exception ex) {
            log.error("Erreur traitement événement kernel Kafka: {}", ex.getMessage());
        }
    }
}
