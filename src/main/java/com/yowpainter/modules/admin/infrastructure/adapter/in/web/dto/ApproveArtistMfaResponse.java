package com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveArtistMfaResponse {
    private String status; // "ACTIVE" or "MFA_REQUIRED"
    private String message;
    private String mfaSessionId;
    private UUID organizationId;
    private UUID kernelActorId;
}
