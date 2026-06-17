package com.yowpainter.shared.kernel;

import org.springframework.http.HttpStatus;

public class KernelPermissionDeniedException extends KernelClientException {

    private final String requiredPermission;

    public KernelPermissionDeniedException(String requiredPermission) {
        super(
            "Le compte technique administrateur ne dispose pas de la permission requise : " + requiredPermission,
            HttpStatus.FORBIDDEN,
            "FORBIDDEN"
        );
        this.requiredPermission = requiredPermission;
    }

    public String getRequiredPermission() {
        return requiredPermission;
    }
}
