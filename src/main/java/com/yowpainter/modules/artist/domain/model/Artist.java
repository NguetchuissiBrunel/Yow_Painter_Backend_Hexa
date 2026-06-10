package com.yowpainter.modules.artist.domain.model;

import com.yowpainter.modules.auth.domain.model.AppUser;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "artist", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Artist extends AppUser {

    // Note concernant l'architecture :
    // Puisque l'application utilise schema-per-tenant, chaque tenant possede son propre schema avec ses propres tables.
    // L'artiste qui a son propre tenant verra son tuple dans la table `artist` du schema courant.
    // Les visiteurs/acheteurs voient un tenant et s'identifient aussi dans le meme schema ou globalement.

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(name = "artist_name", nullable = false)
    private String artistName;

    private String bannerUrl;
    private String location;
    private String status;

    // L'ID du tenant (pour des raisons de tracabilite globale au cas ou la donnee fuit du schema, 
    // ou si on consolide a terme) peut etre ajoute ici.
    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "kernel_user_id")
    private UUID kernelUserId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "payout_phone")
    private String payoutPhone;

    @Column(name = "payout_network")
    private String payoutNetwork;
}
