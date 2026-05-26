package com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderCreateRequest {

    @NotNull(message = "L'ID du produit est requis")
    private UUID productId;

    @Builder.Default
    private int quantity = 1;

    @NotBlank(message = "L'adresse de livraison est requise")
    private String shippingAddress;
}
