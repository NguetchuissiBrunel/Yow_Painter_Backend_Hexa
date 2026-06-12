package com.yowpainter.shared.security;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.model.UserRole;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AuthenticatedUserResolver {

    private final ArtistRepositoryPort artistRepository;
    private final AppUserRepositoryPort appUserRepository;

    public AppUser requireUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("Utilisateur non authentifie");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUser appUser) {
            return appUser;
        }

        throw new IllegalArgumentException(
                "Profil local introuvable. Reconnectez-vous via POST /api/auth/login."
        );
    }

    public Artist requireArtist(Authentication authentication) {
        AppUser user = requireUser(authentication);
        if (user instanceof Artist artist) {
            return artist;
        }
        if (user.getRole() == UserRole.ROLE_ARTIST) {
            return artistRepository.findByEmail(user.getEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Profil artiste incomplet"));
        }
        throw new IllegalArgumentException("Ce compte n'est pas un profil artiste");
    }

    public AppUser requireBuyer(Authentication authentication) {
        AppUser user = requireUser(authentication);
        if (user.getRole() != UserRole.ROLE_BUYER) {
            throw new IllegalArgumentException("Ce compte n'est pas un profil acheteur");
        }
        return user;
    }

    public String requireEmail(Authentication authentication) {
        return requireUser(authentication).getEmail();
    }

    public UUID requireUserId(Authentication authentication) {
        return requireUser(authentication).getId();
    }
}
