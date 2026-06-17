package com.yowpainter.shared.kernel;

import lombok.Getter;

@Getter
public class KernelMfaRequiredException extends RuntimeException {
    private final String mfaToken;

    public KernelMfaRequiredException(String mfaToken) {
        super("Le MFA du Kernel est requis.");
        this.mfaToken = mfaToken;
    }
}
