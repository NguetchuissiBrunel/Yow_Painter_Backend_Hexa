package com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class PendingArtistResponse {
    private UUID id;
    private String email;
    private String firstName;
    private String lastName;
    private String artistName;
    private String slug;
    private String status;
    private UUID kernelUserId;
    private UUID kernelActorId;
    private UUID organizationId;
    private UUID tenantId;
    private LocalDateTime createdAt;
}
