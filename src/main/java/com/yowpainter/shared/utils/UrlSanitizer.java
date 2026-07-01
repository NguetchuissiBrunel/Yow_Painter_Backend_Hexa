package com.yowpainter.shared.utils;

public final class UrlSanitizer {

    private UrlSanitizer() {
    }

    public static String sanitizeFileUrl(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        // Si c'est déjà une URL relative, on la laisse telle quelle
        if (url.startsWith("/api/files/")) {
            return url;
        }
        // Extrait le chemin relatif à partir de /api/files/ si présent
        int index = url.indexOf("/api/files/");
        if (index != -1) {
            return url.substring(index);
        }
        return url;
    }
}
