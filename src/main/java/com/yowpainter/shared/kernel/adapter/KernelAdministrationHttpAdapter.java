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
    private static final String ORGANIZATION_SCOPE = "ORGANIZATION";
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
        assignRole(userId, roleId, null, TENANT_SCOPE, adminToken());
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

        assignRole(userId, organizationAdminRoleId, null, TENANT_SCOPE, adminToken());
    }

    @Override
    public void provisionDefaultRolesForOrganization(UUID organizationId) {
        String adminToken = bootstrapAdminSession.requireAccessToken();
        kernelHttpClient.postList(
                "/api/administration/roles/defaults",
                Map.of(),
                KernelAdministrativeRoleResponseDto.class,
                organizationId,
                adminToken
        );
    }

    @Override
    public void grantOrganizationAdminRole(UUID userId, UUID organizationId) {
        try {
            provisionDefaultRolesForOrganization(organizationId);
        } catch (Exception ex) {
            log.warn("Provision des roles organisation kernel ignoree: {}", ex.getMessage());
        }

        UUID organizationAdminRoleId = listRolesForOrganization(organizationId).stream()
                .filter(role -> ORGANIZATION_ADMIN_ROLE.equalsIgnoreCase(role.code()))
                .map(AdministrativeRoleView::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Role " + ORGANIZATION_ADMIN_ROLE + " introuvable pour l'organisation " + organizationId + "."
                ));

        assignRole(userId, organizationAdminRoleId, organizationId, ORGANIZATION_SCOPE, adminToken());
    }

    private List<AdministrativeRoleView> listRolesForOrganization(UUID organizationId) {
        String adminToken = bootstrapAdminSession.requireAccessToken();
        return kernelHttpClient.getListWithQuery(
                "/api/administration/roles",
                Map.of(),
                KernelAdministrativeRoleResponseDto.class,
                organizationId,
                adminToken
        ).stream()
                .map(dto -> new AdministrativeRoleView(dto.id(), dto.code(), dto.name()))
                .toList();
    }

    private void assignRole(UUID userId, UUID roleId, UUID organizationId, String scope, String adminToken) {
        kernelHttpClient.postVoid(
                "/api/administration/users/" + userId + "/roles",
                new KernelAssignAdministrativeRoleRequestDto(roleId, scope, organizationId, scope),
                organizationId,
                adminToken
        );
    }

    private String adminToken() {
        return bootstrapAdminSession.requireAccessToken();
    }
}
