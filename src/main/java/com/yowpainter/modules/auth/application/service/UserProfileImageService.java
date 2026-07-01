package com.yowpainter.modules.auth.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.ProfileImageUploadResponse;
import com.yowpainter.shared.kernel.KernelClientException;
import com.yowpainter.shared.kernel.port.KernelFilePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileImageService {

    private final AppUserRepositoryPort userRepository;
    private final KernelFilePort kernelFilePort;
    private final ArtistRepositoryPort artistRepository;

    @Transactional
    public ProfileImageUploadResponse uploadProfilePicture(AppUser user, MultipartFile file, String accessToken) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier image requis");
        }

        UUID organizationId = null;
        Object unproxied = org.hibernate.Hibernate.unproxy(user);
        if (unproxied instanceof Artist artist) {
            organizationId = artist.getOrganizationId();
        } else if (user.getRole() == com.yowpainter.modules.auth.domain.model.UserRole.ROLE_ARTIST) {
            organizationId = artistRepository.findByEmail(user.getEmail())
                    .map(Artist::getOrganizationId)
                    .orElse(null);
        }

        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "profile-picture.jpg";
            }
            KernelFilePort.FileView uploaded = kernelFilePort.upload(
                    new KernelFilePort.UploadFileCommand(
                            organizationId,
                            file.getBytes(),
                            fileName,
                            file.getContentType(),
                            "PROFILE_PICTURE"
                    ),
                    accessToken
            );
            user.setProfilePictureUrl(com.yowpainter.shared.utils.UrlSanitizer.sanitizeFileUrl(uploaded.downloadUrl()));
            userRepository.save(user);
            return ProfileImageUploadResponse.builder()
                    .fileId(uploaded.id())
                    .imageUrl(com.yowpainter.shared.utils.UrlSanitizer.sanitizeFileUrl(uploaded.downloadUrl()))
                    .build();
        } catch (KernelClientException ex) {
            throw new IllegalStateException(
                    ex.getMessage() != null ? ex.getMessage() : "Echec upload photo de profil via le kernel",
                    ex
            );
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Impossible de lire le fichier image", ex);
        }
    }

    @Transactional
    public ProfileImageUploadResponse uploadFile(AppUser user, MultipartFile file, String accessToken, String documentType) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Fichier requis");
        }

        UUID organizationId = null;
        Object unproxied = org.hibernate.Hibernate.unproxy(user);
        if (unproxied instanceof Artist artist) {
            organizationId = artist.getOrganizationId();
        } else if (user.getRole() == com.yowpainter.modules.auth.domain.model.UserRole.ROLE_ARTIST) {
            organizationId = artistRepository.findByEmail(user.getEmail())
                    .map(Artist::getOrganizationId)
                    .orElse(null);
        }

        try {
            String fileName = file.getOriginalFilename();
            if (fileName == null || fileName.isBlank()) {
                fileName = "uploaded-file";
            }
            KernelFilePort.FileView uploaded = kernelFilePort.upload(
                    new KernelFilePort.UploadFileCommand(
                            organizationId,
                            file.getBytes(),
                            fileName,
                            file.getContentType(),
                            documentType
                    ),
                    accessToken
            );
            return ProfileImageUploadResponse.builder()
                    .fileId(uploaded.id())
                    .imageUrl(com.yowpainter.shared.utils.UrlSanitizer.sanitizeFileUrl(uploaded.downloadUrl()))
                    .build();
        } catch (KernelClientException ex) {
            throw new IllegalStateException(
                    ex.getMessage() != null ? ex.getMessage() : "Echec upload via le kernel",
                    ex
            );
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Impossible de lire le fichier", ex);
        }
    }
}
