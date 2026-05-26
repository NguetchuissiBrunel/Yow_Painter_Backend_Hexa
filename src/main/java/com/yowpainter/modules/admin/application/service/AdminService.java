package com.yowpainter.modules.admin.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ArtistRepositoryPort artistRepository;
    private final AppUserRepositoryPort userRepository;

    public List<Map<String, Object>> getAllTenants() {
        return artistRepository.findAll().stream().map(artist -> Map.of(
            "id", (Object) artist.getId(),
            "name", (Object) (artist.getArtistName() != null ? artist.getArtistName() : artist.getFirstName() + " " + artist.getLastName()),
            "slug", (Object) artist.getSlug(),
            "status", (Object) artist.getStatus(),
            "email", (Object) artist.getEmail()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void updateTenantStatus(UUID id, String status) {
        Artist artist = artistRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Artiste non trouvé"));
        artist.setStatus(status);
        artistRepository.save(artist);
    }

    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream().map(user -> Map.of(
            "id", (Object) user.getId(),
            "email", (Object) user.getEmail(),
            "firstName", (Object) (user.getFirstName() != null ? user.getFirstName() : ""),
            "lastName", (Object) (user.getLastName() != null ? user.getLastName() : ""),
            "role", (Object) user.getRole().name(),
            "createdAt", (Object) user.getCreatedAt()
        )).collect(Collectors.toList());
    }

    @Transactional
    public void deleteUser(UUID id) {
        if (!userRepository.existsById(id)) {
            throw new IllegalArgumentException("Utilisateur non trouvé");
        }
        userRepository.deleteById(id);
    }

    public Map<String, Object> getGlobalStats() {
        long totalTenants = artistRepository.count();
        long totalUsers = userRepository.count();
        
        return Map.of(
            "total_tenants", totalTenants,
            "total_users", totalUsers,
            "total_sales_volume", 0.0, // FIXME: Nécessite un reporting agrégé multi-tenant
            "active_subscriptions", totalTenants // Hypothèse: 1 artiste = 1 abonnement actif pour l'instant
        );
    }
}
