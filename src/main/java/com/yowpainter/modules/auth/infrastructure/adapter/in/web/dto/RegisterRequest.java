package com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yowpainter.modules.auth.domain.model.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Le prénom est requis")
    private String firstName;

    @NotBlank(message = "Le nom est requis")
    private String lastName;

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    private String password;

    @Schema(allowableValues = {"ROLE_ARTIST", "ROLE_BUYER"})
    private UserRole role; // ROLE_ARTIST ou ROLE_BUYER

    // Spécifique si role = ROLE_ARTIST
    private String artistName;
    @Schema(description = "Slug unique (URL). Sera auto-généré si vide.", example = "jean-dupont")
    private String slug;

    @Schema(name = "imageURL", description = "Deprecated — utiliser POST /api/me/profile-picture apres inscription", deprecated = true)
    @JsonProperty("imageURL")
    private String imageUrl;
}
