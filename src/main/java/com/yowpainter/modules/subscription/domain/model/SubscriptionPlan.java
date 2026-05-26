package com.yowpainter.modules.subscription.domain.model;

import lombok.Getter;
import java.math.BigDecimal;

@Getter
public enum SubscriptionPlan {
    FREE(BigDecimal.ZERO, new BigDecimal("0.15")),    // 15% commission
    PRO(new BigDecimal("10000"), new BigDecimal("0.10")),   // 10% commission
    ELITE(new BigDecimal("25000"), new BigDecimal("0.05")); // 5% commission

    private final BigDecimal price;
    private final BigDecimal commissionRate;
    private final String currency = "XAF";

    SubscriptionPlan(BigDecimal price, BigDecimal commissionRate) {
        this.price = price;
        this.commissionRate = commissionRate;
    }
}
