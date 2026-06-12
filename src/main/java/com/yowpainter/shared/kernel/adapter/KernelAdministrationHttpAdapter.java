package com.yowpainter.shared.kernel.adapter;

import com.yowpainter.shared.kernel.KernelBootstrapAdminSession;
import com.yowpainter.shared.kernel.KernelHttpClient;
import com.yowpainter.shared.kernel.adapter.dto.KernelAdministrativeRoleResponseDto;
import com.yowpainter.shared.kernel.adapter.dto.KernelAssignAdministrativeRoleRequestDto;
import com.yowpainter.shared.kernel.port.KernelAdministrationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class KernelAdministrationHttpAdapter implements KernelAdministrationPort {

    private static final String TENANT_SCOPE = "TENANT";
    private static final String ORGANIZATION_ADMIN_ROLE = "ORGANIZATION_ADMIN";

    private final KernelHttpClient kernelHttpClient;
    private final KernelBootstrapAdminSession bootstrapAdminSession;

    public KernelAdministrationHttpAdapter(
            KernelHttpClient kernelHttpClient,
            KernelBootstrapAdminSession bootstrapAdminSession
    ) {
        this.kernelHttpClient = kernelHttpClient;
        this.bootstrapAdminSession = bootstrapAdminSession;
    }

    @Override
    public List<AdministrativeRoleView> provisionDefaultRoles() {
        String adminToken = bootstrapAdminSession.requireAccessToken();
        return kernelHttpClient.postList(
                "/api/administration/roles/defaults",
                Map.of(),
                KernelAdministrativeRoleResponseDto.class,
                null,
                adminToken
        ).stream()
                .map(dto -> new AdministrativeRoleView(dto.id(), dto.code(), dto.name()))
                .toList();
    }

    @Override
    public List<AdministrativeRoleView> listRoles() {
        String adminToken = bootstrapAdminSession.requireAccessToken();
        return kernelHttpClient.getListWithQuery(
                "/api/administration/roles",
                Map.of(),
                KernelAdministrativeRoleResponseDto.class,
                null,
                adminToken
        ).stream()
                .map(dto -> new AdministrativeRoleView(dto.id(), dto.code(), dto.name()))
                .toList();
    }

    @Override
    public void assignTenantAdminRole(UUID userId, UUID roleId) {
        assignRole(userId, roleId, adminToken());
    }

    @Override
    public void grantOrganizationWriteAccess(UUID userId) {
        try {
            provisionDefaultRoles();
        } catch (Exception ex) {
            log.warn("Provision des roles administratifs kernel ignoree: {}", ex.getMessage());
        }

        UUID organizationAdminRoleId = listRoles().stream()
                .filter(role -> ORGANIZATION_ADMIN_ROLE.equalsIgnoreCase(role.code()))
                .map(AdministrativeRoleView::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + ORGANIZATION_ADMIN_ROLE + " introuvable sur le kernel."
                ));

        assignRole(userId, organizationAdminRoleId, adminToken());
    }

    private void assignRole(UUID userId, UUID roleId, String adminToken) {
        kernelHttpClient.postVoid(
                "/api/administration/users/" + userId + "/roles",
                new KernelAssignAdministrativeRoleRequestDto(roleId, TENANT_SCOPE, null, TENANT_SCOPE),
                null,
                adminToken
        );
    }

    private String adminToken() {
        return bootstrapAdminSession.requireAccessToken();
    }
}
