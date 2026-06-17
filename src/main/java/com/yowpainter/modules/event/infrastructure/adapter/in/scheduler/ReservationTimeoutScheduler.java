package com.yowpainter.modules.event.infrastructure.adapter.in.scheduler;

import com.yowpainter.modules.event.application.service.EventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutScheduler {

    private final EventService eventService;
    private final com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort artistRepository;

    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cleanupAbandonedReservations() {
        log.info("Starting abandoned event reservations cleanup across tenants...");
        
        List<com.yowpainter.modules.artist.domain.model.Artist> activeArtists;
        try {
            activeArtists = artistRepository.findByStatus("ACTIVE");
        } catch (Exception e) {
            log.error("Failed to query active artists for reservation cleanup", e);
            return;
        }

        for (com.yowpainter.modules.artist.domain.model.Artist artist : activeArtists) {
            if (artist.getOrganizationId() == null) {
                continue;
            }
            try {
                com.yowpainter.shared.context.OrganizationContext.setOrganizationId(artist.getOrganizationId());
                eventService.cancelAbandonedReservations();
            } catch (Exception e) {
                log.error("Failed to run reservation cleanup for tenant {}", artist.getOrganizationId(), e);
            } finally {
                com.yowpainter.shared.context.OrganizationContext.clear();
            }
        }
    }
}
