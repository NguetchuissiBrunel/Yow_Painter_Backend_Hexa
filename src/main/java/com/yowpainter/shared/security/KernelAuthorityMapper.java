package com.yowpainter.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class KernelAuthorityMapper {

    private static final Set<String> KERNEL_ADMIN_MARKERS = Set.of(
            "ROLE_TENANT_ADMIN",
            "ROLE_SYSTEM_ADMIN",
            "ROLE_IAM_ADMIN",
            "ROLE_GENERAL_ADMIN"
    );

    private KernelAuthorityMapper() {
    }

    public static boolean isKernelAdminAuthority(String authority) {
        if (authority == null || authority.isBlank()) {
            return false;
        }
        String normalized = authority.toUpperCase();
        return KERNEL_ADMIN_MARKERS.stream().anyMatch(marker ->
                normalized.equals(marker) || normalized.startsWith(marker + "#")
        );
    }

    public static Collection<GrantedAuthority> mapAuthorities(Collection<String> rawAuthorities) {
        LinkedHashSet<GrantedAuthority> mapped = rawAuthorities == null
                ? new LinkedHashSet<>()
                : rawAuthorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        boolean isAdmin = rawAuthorities != null && rawAuthorities.stream().anyMatch(KernelAuthorityMapper::isKernelAdminAuthority);
        if (isAdmin) {
            mapped.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        return mapped;
    }
}
