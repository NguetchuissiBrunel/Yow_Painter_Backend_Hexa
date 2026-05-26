package com.yowpainter.shared.tenant;

public class TenantContext {

    public static final String DEFAULT_TENANT_ID = "public";
    private static final ThreadLocal<String> currentTenant = new ThreadLocal<>();

    public static void setTenantId(String tenantId) {
        currentTenant.set(tenantId);
    }

    public static String getTenantId() {
        return currentTenant.get() != null ? currentTenant.get() : DEFAULT_TENANT_ID;
    }

    public static void clear() {
        currentTenant.remove();
    }

    public static <T> T executeInTenant(String tenantId, java.util.function.Supplier<T> task) {
        String oldTenant = getTenantId();
        setTenantId(tenantId);
        try {
            return task.get();
        } finally {
            setTenantId(oldTenant);
        }
    }
}
