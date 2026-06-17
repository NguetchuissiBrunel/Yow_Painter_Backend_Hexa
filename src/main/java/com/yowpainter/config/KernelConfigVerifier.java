package com.yowpainter.config;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

@Component
public class KernelConfigVerifier {

    private final KernelProperties properties;

    public KernelConfigVerifier(KernelProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void verify() {
        System.out.println("==================================================");
        System.out.println("KERNEL CONFIGURATION VERIFICATION ON STARTUP:");
        System.out.println("KSM_KERNEL_BASE_URL: " + properties.baseUrl());
        System.out.println("KSM_KERNEL_CLIENT_ID: " + properties.clientId());
        System.out.println("KSM_KERNEL_API_KEY: " + (properties.apiKey() != null ? "[MASKED, length=" + properties.apiKey().length() + "]" : "null"));
        System.out.println("KSM_KERNEL_TENANT_ID: " + properties.tenantId());
        System.out.println("KSM_KERNEL_SIGNUP_PLATFORM_ORG_CODE: " + properties.signupPlatformOrganizationCode());
        System.out.println("==================================================");
    }
}
