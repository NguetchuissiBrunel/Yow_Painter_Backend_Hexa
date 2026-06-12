package com.yowpainter.modules.auth.application.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.auth.application.port.out.KernelAuthPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AdminRegisterRequest;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.model.UserRole;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import com.yowpainter.shared.kernel.KernelClientException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String KERNEL_MANAGED_PASSWORD = "{KERNEL_MANAGED}";

    private final AppUserRepositoryPort userRepository;
    private final ArtistRepositoryPort artistRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;
    private final KernelAuthPort kernelAuthPort;
    private final KernelArtistRegistrationService kernelArtistRegistrationService;
    private final KernelAdminRegistrationService kernelAdminRegistrationService;

    public List<String> getAvailableRoles() {
        return List.of(UserRole.ROLE_ARTIST.name(), UserRole.ROLE_BUYER.name());
    }

    public void processForgotPassword(String email) {
        KernelAuthPort.ForgotPasswordResult forgot = kernelAuthPort.forgotPassword(email);
        if (forgot.matchingAccountCount() <= 0 || forgot.contexts().isEmpty()) {
            return;
        }

        KernelAuthPort.PasswordResetContext context = forgot.contexts().get(0);
        KernelAuthPort.IssuedPasswordResetResult issued = kernelAuthPort.issuePasswordReset(
                forgot.selectionToken(),
                context.contextId()
        );

        if ("PREVIEW_ONLY".equalsIgnoreCase(issued.deliveryMode())
                && issued.challengeTokenPreview() != null
                && !issued.challengeTokenPreview().isBlank()) {
            emailService.sendPasswordResetEmail(email, issued.challengeTokenPreview());
        }
    }

    public void resetPassword(String token, String newPassword) {
        kernelAuthPort.resetPassword(token, newPassword);
    }

    @Transactional
    public AuthResponse registerAdmin(AdminRegisterRequest request) {
        return kernelAdminRegistrationService.registerAdmin(request);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getRole() == UserRole.ROLE_ADMIN) {
            throw new IllegalArgumentException("Le role ADMIN ne peut pas etre choisi publiquement");
        }

        if (request.getRole() == UserRole.ROLE_ARTIST) {
            return kernelArtistRegistrationService.registerArtist(request);
        }

        return registerBuyerViaKernel(request);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            KernelAuthPort.KernelLoginResult loginResult = kernelAuthPort.login(
                    request.getEmail(),
                    request.getPassword()
            );
            Artist artist = syncLocalKernelLink(loginResult);
            return KernelAuthMapper.toAuthResponse(loginResult, artist);
        } catch (KernelClientException ex) {
            throw new IllegalArgumentException(
                    ex.getMessage() != null ? ex.getMessage() : "Email ou mot de passe invalide"
            );
        }
    }

    @Transactional
    public AuthResponse refreshToken(String requestToken) {
        KernelAuthPort.KernelLoginResult refreshed = kernelAuthPort.refresh(requestToken);
        Artist artist = syncLocalKernelLink(refreshed);
        return KernelAuthMapper.toAuthResponse(refreshed, artist);
    }

    @Transactional
    public void logout(AppUser user) {
        refreshTokenService.deleteByUserId(user.getId());
    }

    public void logoutWithRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            kernelAuthPort.logout(refreshToken);
        }
    }

    private AuthResponse registerBuyerViaKernel(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Un utilisateur avec cet email existe deja");
        }
        KernelAuthPort.KernelLoginResult signup;
        try {
            signup = kernelAuthPort.signUp(new KernelAuthPort.SignUpCommand(
                    request.getFirstName(),
                    request.getLastName(),
                    request.getEmail(),
                    request.getPassword(),
                    "PROSPECT"
            ));
        } catch (KernelClientException ex) {
            throw new IllegalArgumentException(
                    ex.getMessage() != null ? ex.getMessage() : "Echec inscription via le kernel"
            );
        }
        AppUser buyer = AppUser.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(KERNEL_MANAGED_PASSWORD))
                .role(UserRole.ROLE_BUYER)
                .kernelUserId(signup.userId())
                .build();
        userRepository.save(buyer);
        return KernelAuthMapper.toAuthResponse(signup, null);
    }

    private Artist syncLocalKernelLink(KernelAuthPort.KernelLoginResult loginResult) {
        if (loginResult.userId() != null) {
            Optional<Artist> byKernelUser = artistRepository.findByKernelUserId(loginResult.userId());
            if (byKernelUser.isPresent()) {
                return byKernelUser.get();
            }
            userRepository.findByKernelUserId(loginResult.userId()).ifPresent(user -> {});
        }
        if (loginResult.email() == null) {
            return null;
        }

        Optional<Artist> artist = artistRepository.findByEmail(loginResult.email())
                .map(found -> {
                    linkKernelUserId(found, loginResult.userId());
                    return found;
                });
        if (artist.isPresent()) {
            return artist.get();
        }

        userRepository.findByEmail(loginResult.email())
                .ifPresent(user -> linkKernelUserId(user, loginResult.userId()));
        return null;
    }

    private AppUser linkKernelUserId(AppUser user, UUID kernelUserId) {
        if (kernelUserId == null) {
            return user;
        }
        if (user.getKernelUserId() != null && kernelUserId.equals(user.getKernelUserId())) {
            return user;
        }
        user.setKernelUserId(kernelUserId);
        if (user instanceof Artist artist) {
            return artistRepository.save(artist);
        }
        return userRepository.save(user);
    }
}
