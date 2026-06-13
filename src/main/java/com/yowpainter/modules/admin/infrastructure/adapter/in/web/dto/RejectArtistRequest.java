package com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RejectArtistRequest {

    @Schema(description = "Motif interne (optionnel)")
    private String reason;
}
