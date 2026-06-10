package com.yowpainter.modules.shop.infrastructure.adapter.in.scheduler;

import com.yowpainter.modules.shop.application.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutScheduler {

    private final ShopService shopService;

    @Scheduled(fixedRate = 900000, initialDelay = 60000)
    public void cancelAbandonedOrders() {
        log.info("Starting abandoned orders cleanup...");
        shopService.cancelAbandonedOrders();
    }
}
