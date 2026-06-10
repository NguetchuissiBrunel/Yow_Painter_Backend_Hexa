package com.yowpainter.modules.auth.application.service;

import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.model.UserRole;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AdminRegisterRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.shared.kernel.KernelClientException;
import com.yowpainter.shared.kernel.port.KernelAdministrationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KernelAdminRegistrationService {

    private static final String KERNEL_MANAGED_PASSWORD = "{KERNEL_MANAGED}";
    private static final String TENANT_ADMIN_ROLE_CODE = "TENANT_ADMIN";

    private final KernelAuthPort kernelAuthPort;
    private final KernelAdministrationPort kernelAdministrationPort;
    private final AppUserRepositoryPort userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un administrateur avec cet email existe deja");
        }

        try {
            KernelAuthPort.KernelLoginResult signup = kernelAuthPort.signUp(new KernelAuthPort.SignUpCommand(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword(),
                    "PROSPECT"
            ));

            provisionDefaultRolesQuietly();
            UUID tenantAdminRoleId = resolveTenantAdminRoleId();
            kernelAdministrationPort.assignTenantAdminRole(signup.userId(), tenantAdminRoleId);

            KernelAuthPort.KernelLoginResult loginResult = kernelAuthPort.login(
                    request.getEmail(),
                    request.getPassword()
            );

            AppUser admin = AppUser.builder()
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(KERNEL_MANAGED_PASSWORD))
                    .role(UserRole.ROLE_ADMIN)
                    .kernelUserId(loginResult.userId())
                    .build();
            userRepository.save(admin);

            return KernelAuthMapper.toAuthResponse(loginResult, null);
        } catch (KernelClientException ex) {
            throw new IllegalArgumentException(
                    ex.getMessage() != null ? ex.getMessage() : "Echec inscription administrateur via le kernel"
            );
        }
    }

    private void provisionDefaultRolesQuietly() {
        try {
            kernelAdministrationPort.provisionDefaultRoles();
        } catch (Exception ex) {
            log.debug("Provision des roles administratifs kernel ignoree: {}", ex.getMessage());
        }
    }

    private UUID resolveTenantAdminRoleId() {
        return kernelAdministrationPort.listRoles().stream()
                .filter(role -> TENANT_ADMIN_ROLE_CODE.equalsIgnoreCase(role.code()))
                .map(KernelAdministrationPort.AdministrativeRoleView::id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Role TENANT_ADMIN introuvable sur le kernel. Verifiez la configuration client (X-Api-Key admin)."
                ));
    }
}
