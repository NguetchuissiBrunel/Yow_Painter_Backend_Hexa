package com.yowpainter.modules.admin.application.service;

import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.ApproveArtistRequest;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.ArtistApprovalResponse;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.PendingArtistResponse;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.RejectArtistRequest;
import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.application.service.KernelArtistProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtistApprovalService {

    private static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_REJECTED = "REJECTED";

    private final ArtistRepositoryPort artistRepository;
    private final KernelArtistProvisioningService kernelArtistProvisioningService;

    public List<PendingArtistResponse> listPendingArtists() {
        return artistRepository.findByStatus(STATUS_PENDING_APPROVAL).stream()
                .sorted(Comparator.comparing(Artist::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toPendingResponse)
                .toList();
    }

    @Transactional
    public ArtistApprovalResponse approveArtist(UUID artistId, ApproveArtistRequest request) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));

        if (!STATUS_PENDING_APPROVAL.equalsIgnoreCase(artist.getStatus())) {
            throw new IllegalArgumentException(
                    "Seuls les artistes en attente peuvent etre approuves (statut actuel: " + artist.getStatus() + ")"
            );
        }

        String mfaCode = request != null ? request.getBootstrapMfaCode() : null;
        UUID actorOverride = request != null ? request.getKernelActorId() : null;

        try {
            KernelArtistProvisioningService.ProvisioningResult provisioned =
                    kernelArtistProvisioningService.provisionOnAdminApproval(artist, mfaCode, actorOverride);

            artist.setOrganizationId(provisioned.organizationId());
            artist.setKernelActorId(provisioned.businessActorId());
            if (provisioned.tenantId() != null) {
                artist.setTenantId(provisioned.tenantId());
            }
            artist.setStatus(STATUS_ACTIVE);
            artistRepository.save(artist);

            log.info("Artiste {} approuve (org={}, actor={})", artist.getEmail(), provisioned.organizationId(),
                    provisioned.businessActorId());

            return ArtistApprovalResponse.builder()
                    .artistId(artist.getId())
                    .email(artist.getEmail())
                    .status(STATUS_ACTIVE)
                    .organizationId(provisioned.organizationId())
                    .kernelActorId(provisioned.businessActorId())
                    .message("Artiste approuve. Son espace est actif.")
                    .build();
        } catch (RuntimeException ex) {
            log.warn("Echec approbation artiste {}: {}", artist.getEmail(), ex.getMessage());
            throw new IllegalArgumentException(
                    ex.getMessage() != null ? ex.getMessage() : "Echec du provisioning kernel"
            );
        }
    }

    @Transactional
    public ArtistApprovalResponse rejectArtist(UUID artistId, RejectArtistRequest request) {
        Artist artist = artistRepository.findById(artistId)
                .orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));

        if (!STATUS_PENDING_APPROVAL.equalsIgnoreCase(artist.getStatus())) {
            throw new IllegalArgumentException(
                    "Seuls les artistes en attente peuvent etre refuses (statut actuel: " + artist.getStatus() + ")"
            );
        }

        artist.setStatus(STATUS_REJECTED);
        artistRepository.save(artist);

        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            log.info("Artiste {} refuse: {}", artist.getEmail(), request.getReason());
        } else {
            log.info("Artiste {} refuse", artist.getEmail());
        }

        return ArtistApprovalResponse.builder()
                .artistId(artist.getId())
                .email(artist.getEmail())
                .status(STATUS_REJECTED)
                .message("Demande artiste refusee.")
                .build();
    }

    private PendingArtistResponse toPendingResponse(Artist artist) {
        return PendingArtistResponse.builder()
                .id(artist.getId())
                .email(artist.getEmail())
                .firstName(artist.getFirstName())
                .lastName(artist.getLastName())
                .artistName(artist.getArtistName())
                .slug(artist.getSlug())
                .status(artist.getStatus())
                .kernelUserId(artist.getKernelUserId())
                .kernelActorId(artist.getKernelActorId())
                .organizationId(artist.getOrganizationId())
                .tenantId(artist.getTenantId())
                .createdAt(artist.getCreatedAt())
                .build();
    }
}
