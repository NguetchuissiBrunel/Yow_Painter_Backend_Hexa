package com.yowpainter.modules.artist.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pending_provision_session", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PendingProvisionSession {

    @Id
    private UUID id; // artistId is used as the session ID

    @Column(name = "mfa_token", length = 1000, nullable = false)
    private String mfaToken;

    @Column(name = "kernel_actor_id_override")
    private UUID kernelActorIdOverride;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
