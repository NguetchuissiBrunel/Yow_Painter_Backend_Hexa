package com.yowpainter.modules.auth.infrastructure.adapter.in.web;

import com.yowpainter.modules.auth.application.service.UserProfileImageService;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.ProfileImageUploadResponse;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.shared.security.AuthenticatedUserResolver;
import com.yowpainter.shared.security.KernelAccessTokenResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "Profil utilisateur (fichiers kernel)")
public class UserProfileController {

    private final UserProfileImageService userProfileImageService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @PostMapping(value = "/profile-picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader la photo de profil via le kernel (multipart)")
    public ResponseEntity<ProfileImageUploadResponse> uploadProfilePicture(
            Authentication authentication,
            @RequestParam("file") MultipartFile file) {
        AppUser user = authenticatedUserResolver.requireUser(authentication);
        String accessToken = KernelAccessTokenResolver.requireAccessToken(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileImageService.uploadProfilePicture(user, file, accessToken));
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Uploader un fichier (affiche, image actu, etc.) sous le contexte de l'utilisateur connecté")
    public ResponseEntity<ProfileImageUploadResponse> uploadFile(
            Authentication authentication,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentType", defaultValue = "GENERAL") String documentType) {
        AppUser user = authenticatedUserResolver.requireUser(authentication);
        String accessToken = KernelAccessTokenResolver.requireAccessToken(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userProfileImageService.uploadFile(user, file, accessToken, documentType));
    }
}
