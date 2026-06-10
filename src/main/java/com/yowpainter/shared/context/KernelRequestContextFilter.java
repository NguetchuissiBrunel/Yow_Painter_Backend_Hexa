package com.yowpainter.shared.context;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class KernelRequestContextFilter extends OncePerRequestFilter {

    private static final String ORGANIZATION_HEADER = "X-Organization-Id";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String accessToken = extractBearerToken(request.getHeader(AUTHORIZATION_HEADER));
            UUID organizationId = parseOrganizationId(request.getHeader(ORGANIZATION_HEADER));
            if (organizationId == null) {
                organizationId = parseOrganizationId(request.getParameter("organizationId"));
            }

            if (accessToken != null || organizationId != null) {
                RequestContext.set(new RequestContext.State(accessToken, organizationId, null));
            }
            if (organizationId != null) {
                OrganizationContext.setOrganizationId(organizationId);
            }

            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
            OrganizationContext.clear();
        }
    }

    private String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }
        return authorizationHeader.substring(7);
    }

    private UUID parseOrganizationId(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(rawValue.trim());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
