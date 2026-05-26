package com.yowpainter.shared.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // 1. Si déjà défini par le JwtAuthFilter, on ne fait rien de plus
        if (TenantContext.getTenantId() != null && !TenantContext.getTenantId().equals(TenantContext.DEFAULT_TENANT_ID)) {
            // Déjà défini par JWT
        } else {
            String tenantId = null;
            String requestUri = request.getRequestURI();

            // 2. Extraire du chemin d'URL si c'est une route publique de type /api/v1/public/{artistSlug}/**
            if (requestUri.startsWith("/api/v1/public/")) {
                String[] parts = requestUri.split("/");
                if (parts.length >= 5) {
                    tenantId = parts[4]; // Car / api / v1 / public / {slug}
                }
            }

            // 3. Sinon, lire du header HTTP (cas spécifiques ou admin)
            if (tenantId == null || tenantId.isBlank()) {
                tenantId = request.getHeader(TENANT_HEADER);
            }

            // 4. Définir dans le contexte
            if (tenantId != null && !tenantId.isBlank()) {
                TenantContext.setTenantId(tenantId);
            } else {
                TenantContext.setTenantId(TenantContext.DEFAULT_TENANT_ID);
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
