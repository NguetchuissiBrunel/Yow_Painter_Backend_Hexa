package com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ArtistApprovalResponse {
    private UUID artistId;
    private String email;
    private String status;
    private UUID organizationId;
    private UUID kernelActorId;
    private String message;
}
