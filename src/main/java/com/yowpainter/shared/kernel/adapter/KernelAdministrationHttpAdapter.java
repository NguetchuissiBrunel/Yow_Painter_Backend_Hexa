package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelAdministrativeRoleResponseDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelAssignAdministrativeRoleRequestDto;
import com.yowpainter.shared.kernel.port.KernelAdministrationPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class KernelAdministrationHttpAdapter implements KernelAdministrationPort {

    private static final String TENANT_SCOPE = "TENANT";

    private final KernelHttpClient kernelHttpClient;

    public KernelAdministrationHttpAdapter(KernelHttpClient kernelHttpClient) {
        this.kernelHttpClient = kernelHttpClient;
    }

    @Override
    public void provisionDefaultRoles() {
        kernelHttpClient.postList("/api/administration/roles/defaults", Map.of(), KernelAdministrativeRoleResponseDto.class, null);
    }

    @Override
    public List<AdministrativeRoleView> listRoles() {
        return kernelHttpClient.getListWithQuery(
                "/api/administration/roles",
                Map.of(),
                KernelAdministrativeRoleResponseDto.class,
                null
        ).stream()
                .map(dto -> new AdministrativeRoleView(dto.id(), dto.code(), dto.name()))
                .toList();
    }

    @Override
    public void assignTenantAdminRole(UUID userId, UUID roleId) {
        kernelHttpClient.postVoid(
                "/api/administration/users/" + userId + "/roles",
                new KernelAssignAdministrativeRoleRequestDto(roleId, TENANT_SCOPE, null, TENANT_SCOPE),
                null
        );
    }
}
