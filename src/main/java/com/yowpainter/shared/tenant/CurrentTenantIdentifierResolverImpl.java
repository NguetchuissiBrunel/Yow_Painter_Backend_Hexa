package com.yowpainter.shared.tenant;

import com.yowpainter.shared.context.OrganizationContext;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String> {

    private static final String DEFAULT_TENANT = "public";

    @Override
    public String resolveCurrentTenantIdentifier() {
        java.util.UUID orgId = OrganizationContext.getOrganizationId();
        if (orgId != null) {
            return "tenant_" + orgId.toString().replace("-", "_");
        }
        return DEFAULT_TENANT;
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}
