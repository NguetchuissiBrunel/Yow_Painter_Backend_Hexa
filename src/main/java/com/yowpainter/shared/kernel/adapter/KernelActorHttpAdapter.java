package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelBusinessActorOnboardingRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelBusinessActorResponseDto;
import com.yowpainter.shared.kernel.port.KernelActorPort;
import org.springframework.stereotype.Component;

@Component
public class KernelActorHttpAdapter implements KernelActorPort {

    private final KernelHttpClient kernelHttpClient;

    public KernelActorHttpAdapter(KernelHttpClient kernelHttpClient) {
        this.kernelHttpClient = kernelHttpClient;
    }

    @Override
    public BusinessActorView submitOnboarding(OnboardingCommand command, String accessToken) {
        KernelBusinessActorResponseDto response = kernelHttpClient.post(
                "/api/actors/onboarding",
                new KernelBusinessActorOnboardingRequestDto(
                        command.code(),
                        command.name(),
                        command.businessId(),
                        "ART",
                        "OWNER",
                        true,
                        true,
                        true,
                        true
                ),
                KernelBusinessActorResponseDto.class,
                null,
                accessToken
        );
        return new BusinessActorView(response.id(), response.actorId(), response.code(), response.name());
    }
}
