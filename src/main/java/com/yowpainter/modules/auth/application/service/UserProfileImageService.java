package com.yowpainter.modules.auth.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.ProfileImageUploadResponse;
import com.yowpainter.shared.context.RequestContext;
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

    @Transactional
    public ProfileImageUploadResponse uploadProfilePicture(String userEmail, MultipartFile file) {
        AppUser user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        UUID organizationId = null;
        if (user instanceof Artist artist) {
            organizationId = artist.getOrganizationId();
        }

        try {
            KernelFilePort.FileView uploaded = kernelFilePort.upload(
                    new KernelFilePort.UploadFileCommand(
                            organizationId,
                            file.getBytes(),
                            file.getOriginalFilename(),
                            file.getContentType(),
                            "PROFILE_PICTURE"
                    ),
                    RequestContext.accessToken()
            );
            user.setProfilePictureUrl(uploaded.downloadUrl());
            userRepository.save(user);
            return ProfileImageUploadResponse.builder()
                    .fileId(uploaded.id())
                    .imageUrl(uploaded.downloadUrl())
                    .build();
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("Impossible de lire le fichier image", ex);
        }
    }
}
