package com.yowpainter.shared.kernel.port;

import java.util.List;
import java.util.UUID;

public interface KernelAdministrationPort {

    void provisionDefaultRoles();

    List<AdministrativeRoleView> listRoles();

    void assignTenantAdminRole(UUID userId, UUID roleId);

    record AdministrativeRoleView(UUID id, String code, String name) {
    }
}
