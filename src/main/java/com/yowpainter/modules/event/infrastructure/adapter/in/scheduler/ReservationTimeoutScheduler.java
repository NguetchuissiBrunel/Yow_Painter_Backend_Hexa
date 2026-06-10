package com.yowpainter.modules.event.infrastructure.adapter.in.scheduler;

import com.yowpainter.modules.event.application.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutScheduler {

    private final EventService eventService;

    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cleanupAbandonedReservations() {
        log.info("Starting abandoned event reservations cleanup...");
        eventService.cancelAbandonedReservations();
    }
}
