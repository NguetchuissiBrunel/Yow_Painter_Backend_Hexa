package com.yowpainter.shared.kernel;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KernelApiResponse<T>(
        boolean success,
        T data,
        String message,
        String errorCode,
        Instant timestamp
) {
}
