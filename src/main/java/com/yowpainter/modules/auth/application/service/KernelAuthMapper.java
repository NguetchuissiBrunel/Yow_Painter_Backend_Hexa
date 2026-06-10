package com.yowpainter.modules.auth.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.shared.security.KernelAuthorityMapper;

import java.util.List;
import java.util.UUID;

final class KernelAuthMapper {

    private KernelAuthMapper() {
    }

    static AuthResponse toAuthResponse(KernelAuthPort.KernelLoginResult loginResult, Artist artist) {
        UUID organizationId = resolveOrganizationId(loginResult, artist);
        return AuthResponse.builder()
                .accessToken(loginResult.accessToken())
                .refreshToken(loginResult.refreshToken())
                .email(loginResult.email())
                .firstName(artist != null ? artist.getFirstName() : null)
                .lastName(artist != null ? artist.getLastName() : null)
                .imageUrl(artist != null ? artist.getProfilePictureUrl() : null)
                .role(resolveRole(loginResult, artist))
                .tenantId(loginResult.tenantId() != null ? loginResult.tenantId().toString() : null)
                .artistName(artist != null ? artist.getArtistName() : null)
                .kernelUserId(loginResult.userId())
                .organizationId(organizationId)
                .organizations(mapOrganizations(loginResult.organizations()))
                .build();
    }

    private static UUID resolveOrganizationId(KernelAuthPort.KernelLoginResult loginResult, Artist artist) {
        if (artist != null && artist.getOrganizationId() != null) {
            return artist.getOrganizationId();
        }
        if (loginResult.organizations() != null && loginResult.organizations().size() == 1) {
            return loginResult.organizations().get(0).organizationId();
        }
        return null;
    }

    private static String resolveRole(KernelAuthPort.KernelLoginResult loginResult, Artist artist) {
        if (artist != null) {
            return artist.getRole().name();
        }
        if (loginResult.authorities() != null
                && loginResult.authorities().stream().anyMatch(KernelAuthorityMapper::isKernelAdminAuthority)) {
            return "ROLE_ADMIN";
        }
        if (loginResult.authorities() != null) {
            return loginResult.authorities().stream().findFirst().orElse("ROLE_BUYER");
        }
        return "ROLE_BUYER";
    }

    private static List<AuthResponse.OrganizationAccessResponse> mapOrganizations(
            List<KernelAuthPort.KernelOrganizationAccess> organizations
    ) {
        if (organizations == null) {
            return List.of();
        }
        return organizations.stream()
                .map(org -> AuthResponse.OrganizationAccessResponse.builder()
                        .organizationId(org.organizationId())
                        .organizationCode(org.organizationCode())
                        .displayName(org.displayName() != null ? org.displayName() : org.shortName())
                        .services(org.services())
                        .build())
                .toList();
    }
}
