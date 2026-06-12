package com.yowpainter.shared.security;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class KernelJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final ArtistRepositoryPort artistRepository;
    private final AppUserRepositoryPort appUserRepository;
    private final KernelAuthPort kernelAuthPort;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> kernelAuthorities = KernelAuthorityMapper.resolveFromJwt(jwt);
        UUID kernelUserId = parseKernelUserId(jwt);

        if (kernelUserId != null) {
            Optional<AppUser> localUser = resolveLocalUser(kernelUserId, jwt.getTokenValue());
            if (localUser.isPresent()) {
                return authenticatedUser(localUser.get(), jwt, authoritiesForUser(localUser.get(), kernelAuthorities));
            }
        }

        return new JwtAuthenticationToken(jwt, kernelAuthorities, jwt.getSubject());
    }

    private Optional<AppUser> resolveLocalUser(UUID kernelUserId, String accessToken) {
        Optional<Artist> artist = artistRepository.findByKernelUserId(kernelUserId);
        if (artist.isPresent()) {
            return Optional.of(artist.get());
        }

        Optional<AppUser> appUser = appUserRepository.findByKernelUserId(kernelUserId);
        if (appUser.isPresent()) {
            return appUser;
        }

        return linkLocalUserFromKernelProfile(accessToken, kernelUserId);
    }

    private Optional<AppUser> linkLocalUserFromKernelProfile(String accessToken, UUID kernelUserId) {
        try {
            KernelAuthPort.KernelUserProfile profile = kernelAuthPort.me(accessToken);
            if (profile.email() == null || profile.email().isBlank()) {
                return Optional.empty();
            }

            Optional<Artist> artist = artistRepository.findByEmail(profile.email());
            if (artist.isPresent()) {
                return Optional.of(persistKernelUserId(artist.get(), kernelUserId));
            }

            return appUserRepository.findByEmail(profile.email())
                    .map(user -> persistKernelUserId(user, kernelUserId));
        } catch (Exception ex) {
            log.debug("Liaison profil local via kernel /me ignoree: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private AppUser persistKernelUserId(AppUser user, UUID kernelUserId) {
        if (user.getKernelUserId() != null && kernelUserId.equals(user.getKernelUserId())) {
            return user;
        }
        user.setKernelUserId(kernelUserId);
        if (user instanceof Artist artist) {
            return artistRepository.save(artist);
        }
        return appUserRepository.save(user);
    }

    private static Collection<GrantedAuthority> authoritiesForUser(
            AppUser user,
            Collection<GrantedAuthority> kernelAuthorities
    ) {
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>(kernelAuthorities);
        if (user.getRole() != null) {
            authorities.add(new SimpleGrantedAuthority(user.getRole().name()));
        } else if (user instanceof Artist) {
            authorities.add(new SimpleGrantedAuthority(com.yowpainter.modules.auth.domain.model.UserRole.ROLE_ARTIST.name()));
        }
        return authorities;
    }

    private static UsernamePasswordAuthenticationToken authenticatedUser(
            AppUser user,
            Jwt jwt,
            Collection<GrantedAuthority> authorities
    ) {
        return new UsernamePasswordAuthenticationToken(user, jwt, authorities);
    }

    private static UUID parseKernelUserId(Jwt jwt) {
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
