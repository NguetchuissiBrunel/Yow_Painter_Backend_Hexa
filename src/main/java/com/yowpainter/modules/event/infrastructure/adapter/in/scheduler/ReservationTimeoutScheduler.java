package com.yowpainter.modules.event.infrastructure.adapter.in.scheduler;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.event.application.service.EventService;
import com.yowpainter.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationTimeoutScheduler {

    private final EventService eventService;
    private final ArtistRepositoryPort artistRepository;
    
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    // Run every 15 minutes, start after 1 minute to allow server to bind port
    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cleanupAbandonedReservations() {
        log.info("Starting Parallel Multi-Tenant Event Reservations Cleanup...");
        
        List<Artist> artists = artistRepository.findAll();
        
        for (Artist artist : artists) {
            String tenantId = artist.getSlug();
            CompletableFuture.runAsync(() -> {
                try {
                    TenantContext.executeInTenant(tenantId, () -> {
                        eventService.cancelAbandonedReservationsForTenant(tenantId);
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed to cleanup reservations for tenant: {}", tenantId, e);
                }
            }, taskExecutor);
        }
    }
}
