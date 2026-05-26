package com.yowpainter.modules.event.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    // QR Code data crypté ou signé (ex: jwt token léger ou hash unique)
    @Column(name = "qr_code_data", unique = true, nullable = false)
    private String qrCodeData;

    @Column(name = "qr_code_image_url")
    private String qrCodeImageUrl;

    @Column(name = "is_scanned", nullable = false)
    @Builder.Default
    private boolean isScanned = false;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;
}
