package com.yowpainter.shared.kernel.port;

import java.util.List;
import java.util.UUID;

public interface KernelAdministrationPort {

    List<AdministrativeRoleView> provisionDefaultRoles();

    List<AdministrativeRoleView> listRoles();

    void assignTenantAdminRole(UUID userId, UUID roleId);

    void grantOrganizationWriteAccess(UUID userId);

    record AdministrativeRoleView(UUID id, String code, String name) {
    }
}
