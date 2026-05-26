package com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateRequest {

    // Optionnel, on passe l'ID de l'oeuvre si on vend la toile certifiee
    private UUID artworkId;

    @NotBlank(message = "Le nom du produit est requis")
    private String name;

    private String description;

    @NotNull(message = "Le prix est requis")
    @DecimalMin(value = "0.01", message = "Le prix doit etre superieur a 0")
    private BigDecimal price;

    @Builder.Default
    private int stockQuantity = 1;
}
