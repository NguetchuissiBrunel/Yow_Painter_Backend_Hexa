package com.yowpainter.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.yowpainter.shared.kernel.KernelClientException;

import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Requete invalide";
        HttpStatus status = HttpStatus.BAD_REQUEST;
        if (message.contains("non trouve") || message.contains("introuvable")) {
            status = HttpStatus.NOT_FOUND;
        } else if (message.contains("non authentifie")) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (message.contains("Profil local introuvable") || message.contains("pas un profil")
                || message.contains("n'appartient pas")) {
            status = HttpStatus.FORBIDDEN;
        }
        return ResponseEntity.status(status).body(Map.of("message", message));
    }

    @ExceptionHandler({IllegalStateException.class, KernelClientException.class})
    public ResponseEntity<Map<String, String>> handleKernelConfiguration(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Configuration kernel invalide";
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("message", message));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Acces refuse"));
    }
}
