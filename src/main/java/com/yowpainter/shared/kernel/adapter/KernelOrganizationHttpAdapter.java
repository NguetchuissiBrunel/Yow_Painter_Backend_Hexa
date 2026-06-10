package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelApplyCommercialPlanRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelCreateOrganizationRequestDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelOrganizationResponseDto;
import com.yowpainter.shared.kernel.port.KernelOrganizationPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KernelOrganizationHttpAdapter implements KernelOrganizationPort {

    private final KernelHttpClient kernelHttpClient;
    private final KernelProperties properties;

    public KernelOrganizationHttpAdapter(KernelHttpClient kernelHttpClient, KernelProperties properties) {
        this.kernelHttpClient = kernelHttpClient;
        this.properties = properties;
    }

    @Override
    public OrganizationView createOrganization(CreateOrganizationCommand command, String accessToken) {
        KernelOrganizationResponseDto response = kernelHttpClient.post(
                "/api/organizations",
                new KernelCreateOrganizationRequestDto(
                        command.businessActorId(),
                        command.code(),
                        "FREELANCE",
                        true,
                        command.email(),
                        command.shortName(),
                        command.longName()
                ),
                KernelOrganizationResponseDto.class,
                null,
                accessToken
        );
        return new OrganizationView(response.id(), response.businessActorId(), response.code(),
                response.shortName(), response.longName());
    }

    @Override
    public void applyCommercialPlan(UUID organizationId, String planCode, String accessToken) {
        kernelHttpClient.post(
                "/api/organizations/" + organizationId + "/commercial-subscriptions",
                new KernelApplyCommercialPlanRequestDto(
                        planCode != null && !planCode.isBlank() ? planCode : properties.defaultPlanCode()
                ),
                Object.class,
                organizationId,
                accessToken
        );
    }
}
