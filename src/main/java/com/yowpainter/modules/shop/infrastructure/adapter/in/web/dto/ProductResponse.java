package com.yowpainter.modules.shop.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private UUID artistId;
    private UUID artworkId;
    private String name;
    private String description;
    private BigDecimal price;
    private int stockQuantity;
    private boolean isActive;
}
