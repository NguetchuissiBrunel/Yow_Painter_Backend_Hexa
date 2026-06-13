package com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmEmailRequest {

    @NotBlank(message = "Le token de verification est requis")
    private String token;
}
