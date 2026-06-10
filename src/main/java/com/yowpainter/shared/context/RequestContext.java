package com.yowpainter.shared.context;

import java.util.UUID;

public final class RequestContext {

    private static final ThreadLocal<State> current = new ThreadLocal<>();

    private RequestContext() {
    }

    public static void set(State state) {
        current.set(state);
    }

    public static State get() {
        return current.get();
    }

    public static String accessToken() {
        State state = current.get();
        return state == null ? null : state.accessToken();
    }

    public static UUID organizationId() {
        State state = current.get();
        return state == null ? null : state.organizationId();
    }

    public static void clear() {
        current.remove();
    }

    public record State(String accessToken, UUID organizationId, String kernelTenantId) {
    }
}
