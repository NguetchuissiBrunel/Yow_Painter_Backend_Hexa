package com.yowpainter.shared.kernel.port;

import java.util.UUID;

public interface KernelActorPort {

    BusinessActorView submitOnboarding(OnboardingCommand command, String accessToken);

    record OnboardingCommand(
            String code,
            String name,
            String businessId,
            String email
    ) {
    }

    record BusinessActorView(UUID id, UUID actorId, String code, String name) {
    }
}
