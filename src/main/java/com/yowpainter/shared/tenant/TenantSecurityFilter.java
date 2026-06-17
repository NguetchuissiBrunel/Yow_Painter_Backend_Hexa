package com.yowpainter.shared.tenant;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.shared.context.OrganizationContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class TenantSecurityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        boolean setHere = false;
        try {
            if (OrganizationContext.getOrganizationId() == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    Object principal = authentication.getPrincipal();
                    if (principal instanceof Artist artist) {
                        if (artist.getOrganizationId() != null) {
                            OrganizationContext.setOrganizationId(artist.getOrganizationId());
                            setHere = true;
                        }
                    }
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            if (setHere) {
                OrganizationContext.clear();
            }
        }
    }
}
