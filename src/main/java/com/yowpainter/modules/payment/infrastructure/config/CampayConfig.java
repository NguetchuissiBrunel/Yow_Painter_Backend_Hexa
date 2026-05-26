package com.yowpainter.modules.payment.infrastructure.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class CampayConfig {

    @Value("${app.payment.campay.app-username}")
    private String appUsername;

    @Value("${app.payment.campay.app-password}")
    private String appPassword;

    @Value("${app.payment.campay.base-url:https://demo.campay.net/api}")
    private String baseUrl;

    @Value("${app.payment.campay.token-url:/token/}")
    private String tokenUrl;

    @Value("${app.payment.campay.collect-url:/collect/}")
    private String collectUrl;

    @Value("${app.payment.campay.transaction-url:/transaction/}")
    private String transactionUrl;

    @Value("${app.payment.campay.withdraw-url:/withdraw/}")
    private String withdrawUrl;
}
