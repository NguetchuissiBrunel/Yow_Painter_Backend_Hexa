package com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfilePictureRequest {

    @Deprecated
    @Schema(description = "Deprecated — utiliser POST /api/auth/me/profile-picture (multipart kernel)")
    private String profilePictureUrl;
}
