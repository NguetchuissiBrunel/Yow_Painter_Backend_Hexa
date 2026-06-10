package com.yowpainter.modules.auth.application.service;

import com.yowpainter.config.KernelProperties;
import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.modules.auth.domain.model.UserRole;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.yowpainter.shared.kernel.port.KernelOrganizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class KernelArtistRegistrationService {

    private static final String KERNEL_MANAGED_PASSWORD = "{KERNEL_MANAGED}";

    private final KernelAuthPort kernelAuthPort;
    private final KernelOrganizationPort kernelOrganizationPort;
    private final ArtistRepositoryPort artistRepository;
    private final PasswordEncoder passwordEncoder;
    private final KernelProperties kernelProperties;

    @Transactional
    public AuthResponse registerArtist(RegisterRequest request) {
        if (artistRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe deja");
        }

        String slug = resolveSlug(request);
        KernelAuthPort.KernelLoginResult signup = kernelAuthPort.signUp(new KernelAuthPort.SignUpCommand(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getPassword(),
                "BUSINESS"
        ));

        String artistName = request.getArtistName() != null && !request.getArtistName().isBlank()
                ? request.getArtistName()
                : request.getFirstName() + " " + request.getLastName();

        KernelOrganizationPort.OrganizationView organization = kernelOrganizationPort.createOrganization(
                new KernelOrganizationPort.CreateOrganizationCommand(
                        signup.actorId(),
                        slug,
                        artistName,
                        artistName,
                        request.getEmail()
                ),
                signup.accessToken()
        );

        kernelOrganizationPort.applyCommercialPlan(
                organization.id(),
                kernelProperties.defaultPlanCode(),
                signup.accessToken()
        );

        Artist artist = Artist.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(KERNEL_MANAGED_PASSWORD))
                .role(UserRole.ROLE_ARTIST)
                .artistName(artistName)
                .slug(slug)
                .status("ACTIVE")
                .kernelUserId(signup.userId())
                .organizationId(organization.id())
                .tenantId(organization.id())
                .build();

        artistRepository.save(artist);
        return KernelAuthMapper.toAuthResponse(signup, artist);
    }

    private String resolveSlug(RegisterRequest request) {
        String slug = request.getSlug();
        if (slug == null || slug.isBlank()) {
            String baseName = request.getArtistName() != null && !request.getArtistName().isBlank()
                    ? request.getArtistName()
                    : request.getFirstName() + " " + request.getLastName();
            slug = generateSlug(baseName);
        } else {
            slug = generateSlug(slug);
        }
        if (artistRepository.findBySlug(slug).isPresent()) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 5);
        }
        return slug;
    }

    private String generateSlug(String input) {
        if (input == null || input.isBlank()) {
            return "artist";
        }
        return input.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }
}
