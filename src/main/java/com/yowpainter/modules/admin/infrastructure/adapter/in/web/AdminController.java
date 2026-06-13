package com.yowpainter.modules.admin.infrastructure.adapter.in.web;

import com.yowpainter.modules.admin.application.service.ArtistApprovalService;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.ApproveArtistRequest;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.ArtistApprovalResponse;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.PendingArtistResponse;
import com.yowpainter.modules.admin.infrastructure.adapter.in.web.dto.RejectArtistRequest;
import com.yowpainter.shared.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Administration", description = "Controles globaux de la plateforme (Restreint aux Admins)")
public class AdminController {

    private final com.yowpainter.modules.admin.application.service.AdminService adminService;
    private final ArtistApprovalService artistApprovalService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping("/artists/pending")
    @Operation(summary = "Lister les artistes en attente de validation")
    public ResponseEntity<List<PendingArtistResponse>> listPendingArtists() {
        return ResponseEntity.ok(artistApprovalService.listPendingArtists());
    }

    @PostMapping("/artists/{id}/approve")
    @Operation(summary = "Approuver un artiste et provisionner son espace Kernel")
    public ResponseEntity<ArtistApprovalResponse> approveArtist(
            @PathVariable UUID id,
            @RequestBody(required = false) ApproveArtistRequest request
    ) {
        try {
            return ResponseEntity.ok(artistApprovalService.approveArtist(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/artists/{id}/reject")
    @Operation(summary = "Refuser une demande artiste")
    public ResponseEntity<ArtistApprovalResponse> rejectArtist(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectArtistRequest request
    ) {
        try {
            return ResponseEntity.ok(artistApprovalService.rejectArtist(id, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/tenants")
    @Operation(summary = "Lister tous les artistes / tenants enregistres")
    public ResponseEntity<List<Map<String, Object>>> getAllTenants() {
        return ResponseEntity.ok(adminService.getAllTenants());
    }

    @PatchMapping("/tenants/{id}/status")
    @Operation(summary = "Activer ou suspendre un tenant")
    public ResponseEntity<Void> updateTenantStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            adminService.updateTenantStatus(id, status);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/users")
    @Operation(summary = "Lister tous les utilisateurs de la plateforme")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @DeleteMapping("/users/{id}")
    @Operation(summary = "Supprimer definitivement un utilisateur")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        try {
            adminService.deleteUser(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Statistiques globales de la plateforme")
    public ResponseEntity<Map<String, Object>> getGlobalStats() {
        return ResponseEntity.ok(adminService.getGlobalStats());
    }

    @GetMapping("/logs")
    @Operation(summary = "Consulter les logs d'audit (Mock)")
    public ResponseEntity<List<String>> getAuditLogs() {
        // En attente d'implémentation de la table AuditLog
        return ResponseEntity.ok(List.of("Fonctionnalité en cours de développement"));
    }

    @GetMapping("/me")
    @Operation(summary = "Récupérer le profil de l'administrateur connecté")
    public ResponseEntity<Map<String, String>> getMe(Authentication authentication) {
        var user = authenticatedUserResolver.requireUser(authentication);
        return ResponseEntity.ok(Map.of(
            "email", user.getEmail(),
            "role", user.getRole().name()
        ));
    }
}
