package com.yowpainter.modules.shop.domain.model;

import com.yowpainter.modules.artwork.domain.model.Artwork;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "artist_id", nullable = false)
    private UUID artistId;

    // Lien optionnel vers l'œuvre d'art si le produit EST la toile originale
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artwork_id", unique = true)
    private Artwork artwork;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    // 1 si l'oeuvre est unique, > 1 si c'est de l'imprimerie/marchandise
    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default
    private int stockQuantity = 1;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "kernel_product_id")
    private UUID kernelProductId;

    @Column(name = "organization_id")
    private UUID organizationId;
}
