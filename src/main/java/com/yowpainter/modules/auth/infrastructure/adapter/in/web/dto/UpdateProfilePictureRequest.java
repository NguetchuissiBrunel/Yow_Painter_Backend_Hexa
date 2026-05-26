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

    @NotBlank(message = "L'URL de la photo de profil est requise")
    @Schema(description = "URL Cloudinary de la photo de profil", example = "https://res.cloudinary.com/...")
    private String profilePictureUrl;
}
