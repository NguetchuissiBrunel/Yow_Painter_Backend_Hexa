package com.yowpainter.modules.auth.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.BuyerUpdateRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.BuyerProfileResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/buyer")
@RequiredArgsConstructor
@Tag(name = "Buyer Profile", description = "Gestion du profil Acheteur")
public class BuyerController {

    private final AppUserRepositoryPort userRepository;

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'acheteur connecté")
    public ResponseEntity<BuyerProfileResponse> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        return ResponseEntity.ok(mapToResponse(user));
    }

    @PutMapping("/me")
    @Operation(summary = "Mettre à jour le profil de l'acheteur")
    public ResponseEntity<BuyerProfileResponse> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody BuyerUpdateRequest request) {
        AppUser user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setBio(request.getBio());
        
        return ResponseEntity.ok(mapToResponse(userRepository.save(user)));
    }

    private BuyerProfileResponse mapToResponse(AppUser user) {
        return BuyerProfileResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .profilePictureUrl(user.getProfilePictureUrl())
                .bio(user.getBio())
                .role(user.getRole().name())
                .build();
    }
}
