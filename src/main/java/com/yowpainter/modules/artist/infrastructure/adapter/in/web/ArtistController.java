package com.yowpainter.modules.artist.infrastructure.adapter.in.web;

import com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto.ArtistAnalyticsResponse;
import com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto.ArtistResponse;
import com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto.ArtistUpdateRequest;
import com.yowpainter.modules.artist.application.service.ArtistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Artists", description = "Endpoints de gestion des profils artistes et recherche")
public class ArtistController {

    private final ArtistService artistService;

    @GetMapping("/public/artists/{slug}")
    @Operation(summary = "Recuperer le profil public d'un artiste par son slug")
    public ResponseEntity<ArtistResponse> getArtistBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(artistService.getArtistBySlug(slug));
    }

    @GetMapping("/public/artists/search")
    @Operation(summary = "Rechercher des artistes par nom ou slug")
    public ResponseEntity<List<ArtistResponse>> searchArtists(@RequestParam String q) {
        return ResponseEntity.ok(artistService.searchArtists(q));
    }

    @GetMapping("/artist/me")
    @Operation(summary = "Recuperer mon propre profil (Artiste connecte)")
    public ResponseEntity<ArtistResponse> getMyProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(artistService.getArtistByEmail(userDetails.getUsername()));
    }

    @PutMapping("/artist/me")
    @Operation(summary = "Mettre a jour mon profil artiste")
    public ResponseEntity<ArtistResponse> updateMyProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ArtistUpdateRequest request) {
        return ResponseEntity.ok(artistService.updateArtist(userDetails.getUsername(), request));
    }

    @GetMapping("/artist/me/analytics")
    @Operation(summary = "Recuperer les statistiques de mon dashboard")
    public ResponseEntity<ArtistAnalyticsResponse> getMyAnalytics(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(artistService.getArtistAnalytics(userDetails.getUsername()));
    }

    @GetMapping("/public/artists/id/{id}")
    @Operation(summary = "Recuperer un artiste par son ID")
    public ResponseEntity<ArtistResponse> getArtistById(@PathVariable UUID id) {
        return ResponseEntity.ok(artistService.getArtistById(id));
    }
}
