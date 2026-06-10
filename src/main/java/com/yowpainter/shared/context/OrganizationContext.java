package com.yowpainter.shared.context;

import java.util.UUID;

public final class OrganizationContext {

    private static final ThreadLocal<UUID> currentOrganization = new ThreadLocal<>();

    private OrganizationContext() {
    }

    public static void setOrganizationId(UUID organizationId) {
        currentOrganization.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return currentOrganization.get();
    }

    public static void clear() {
        currentOrganization.remove();
    }
}
