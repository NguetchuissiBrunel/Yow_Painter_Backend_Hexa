package com.yowpainter.modules.shop.infrastructure.adapter.in.scheduler;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.artwork.domain.model.Artwork;
import com.yowpainter.modules.artwork.domain.port.out.ArtworkRepositoryPort;
import com.yowpainter.modules.shop.domain.model.Order;
import com.yowpainter.modules.shop.domain.model.OrderItem;
import com.yowpainter.modules.shop.domain.model.OrderStatus;
import com.yowpainter.modules.shop.domain.model.Product;
import com.yowpainter.modules.shop.domain.port.out.OrderRepositoryPort;
import com.yowpainter.modules.shop.domain.port.out.ProductRepositoryPort;
import com.yowpainter.modules.shop.application.service.ShopService;
import com.yowpainter.shared.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final ShopService shopService;
    private final ArtistRepositoryPort artistRepository;
    
    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    // Run every 15 minutes, start after 1 minute
    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cancelAbandonedOrders() {
        log.info("Starting Parallel Multi-Tenant Abandoned Orders Cleanup...");
        
        List<Artist> artists = artistRepository.findAll();
        
        for (Artist artist : artists) {
            String tenantId = artist.getSlug();
            CompletableFuture.runAsync(() -> {
                try {
                    TenantContext.executeInTenant(tenantId, () -> {
                        shopService.cancelAbandonedOrdersForTenant(tenantId);
                        return null;
                    });
                } catch (Exception e) {
                    log.error("Failed to cleanup orders for tenant: {}", tenantId, e);
                }
            }, taskExecutor);
        }
    }
}
