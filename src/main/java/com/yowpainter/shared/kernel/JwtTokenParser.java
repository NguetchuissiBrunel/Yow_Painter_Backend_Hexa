package com.yowpainter.shared.kernel;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
public class JwtTokenParser {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public record JwtTokenInfo(
        UUID actorId,
        UUID userId,
        UUID tenantId,
        List<String> roles,
        List<String> permissions,
        boolean adm
    ) {}

    public static JwtTokenInfo parseToken(String token) {
        if (token == null || token.isBlank()) {
            return new JwtTokenInfo(null, null, null, List.of(), List.of(), false);
        }
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            return new JwtTokenInfo(null, null, null, List.of(), List.of(), false);
        }
        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode node = objectMapper.readTree(payload);

            UUID actorId = node.has("actor") && !node.get("actor").isNull() ? UUID.fromString(node.get("actor").asText()) : null;
            UUID userId = node.has("sub") && !node.get("sub").isNull() ? UUID.fromString(node.get("sub").asText()) : null;
            UUID tenantId = node.has("tid") && !node.get("tid").isNull() ? UUID.fromString(node.get("tid").asText()) : null;

            boolean adm = node.has("adm") && node.get("adm").asBoolean(false);

            List<String> roles = new ArrayList<>();
            List<String> permissions = new ArrayList<>();
            JsonNode permsNode = node.get("permissions");
            if (permsNode != null && permsNode.isArray()) {
                for (JsonNode p : permsNode) {
                    String val = p.asText();
                    if (val.startsWith("ROLE_")) {
                        roles.add(val);
                    } else {
                        permissions.add(val);
                    }
                }
            }

            return new JwtTokenInfo(actorId, userId, tenantId, roles, permissions, adm);
        } catch (Exception ex) {
            log.warn("[JwtTokenParser] Error parsing token: {}", ex.getMessage());
            return new JwtTokenInfo(null, null, null, List.of(), List.of(), false);
        }
    }
}
