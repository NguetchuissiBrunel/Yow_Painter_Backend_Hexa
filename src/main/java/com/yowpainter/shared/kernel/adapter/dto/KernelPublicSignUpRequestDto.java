package com.yowpainter.shared.kernel.adapter.dto;

import java.util.Map;
import java.util.UUID;

public record KernelPublicSignUpRequestDto(
        UUID tenantId,
        String signUpSelectionToken,
        String contextId,
        String firstName,
        String lastName,
        String username,
        String email,
        String phoneNumber,
        String password,
        String socialProvider,
        String externalSubject,
        String captchaVerificationToken,
        String accountType,
        String businessType,
        Map<String, Object> onboardingData
) {
}
