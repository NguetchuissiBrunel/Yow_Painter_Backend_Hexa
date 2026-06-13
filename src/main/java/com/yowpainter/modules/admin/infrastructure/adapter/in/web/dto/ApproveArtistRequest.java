package com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class ApproveArtistRequest {

    @Schema(description = "Code MFA e-mail du compte bootstrap platform-admin (requis en prod)")
    private String bootstrapMfaCode;

    @Schema(description = "Identifiant acteur Kernel si absent du profil local")
    private UUID kernelActorId;
}
