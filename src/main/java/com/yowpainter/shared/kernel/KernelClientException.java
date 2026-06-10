package com.yowpainter.shared.kernel;

import org.springframework.http.HttpStatusCode;

public class KernelClientException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final String errorCode;

    public KernelClientException(String message, HttpStatusCode statusCode, String errorCode) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public String errorCode() {
        return errorCode;
    }
}
