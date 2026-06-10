package com.yowpainter.modules.auth.infrastructure.adapter.in.web;

import java.util.List;

import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.RefreshTokenRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.yowpainter.modules.auth.application.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints pour s'inscrire et se connecter")
public class AuthController {

    private final AuthService authService;

    @GetMapping("/roles")
    @Operation(summary = "Lister les roles disponibles pour l'inscription")
    public ResponseEntity<List<String>> getRoles() {
        return ResponseEntity.ok(authService.getAvailableRoles());
    }

    @PostMapping("/register")
    @Operation(summary = "Inscription d'un nouvel utilisateur (Artiste ou Acheteur)")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        if (request.getRole() == com.yowpainter.modules.auth.domain.model.UserRole.ROLE_ADMIN) {
            return ResponseEntity.badRequest().build();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .message(e.getMessage())
                    .build()); 
        }
    }

    @PostMapping("/login")
    @Operation(summary = "Connexion et recuperation du token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rafraichir le token JWT")
    public ResponseEntity<AuthResponse> refresh(
            @RequestParam(required = false) String refreshToken,
            @Valid @RequestBody(required = false) RefreshTokenRequest body
    ) {
        String token = body != null && body.getRefreshToken() != null ? body.getRefreshToken() : refreshToken;
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .message("refreshToken est requis")
                    .build());
        }
        try {
            return ResponseEntity.ok(authService.refreshToken(token));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(AuthResponse.builder()
                    .message(e.getMessage())
                    .build());
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Se deconnecter")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody(required = false) RefreshTokenRequest body
    ) {
        if (body != null && body.getRefreshToken() != null) {
            authService.logoutWithRefreshToken(body.getRefreshToken());
        }
        if (userDetails instanceof com.yowpainter.modules.auth.domain.model.AppUser) {
            authService.logout((com.yowpainter.modules.auth.domain.model.AppUser) userDetails);
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Demander la réinitialisation du mot de passe")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.ForgotPasswordRequest request) {
        try {
            authService.processForgotPassword(request.getEmail());
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Si un compte existe pour cet e-mail, un lien de réinitialisation a été envoyé.")
                    .build());
        } catch (IllegalArgumentException e) {
            // Pour la sécurité, on retourne la même réponse même si l'e-mail n'existe pas
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Si un compte existe pour cet e-mail, un lien de réinitialisation a été envoyé.")
                    .build());
        }
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Réinitialiser le mot de passe avec le jeton")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.ResetPasswordRequest request) {
        try {
            authService.resetPassword(request.getToken(), request.getNewPassword());
            return ResponseEntity.ok(AuthResponse.builder()
                    .message("Votre mot de passe a été réinitialisé avec succès.")
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(AuthResponse.builder()
                    .message(e.getMessage())
                    .build());
        }
    }


}
