package com.yowpainter.modules.admin.infrastructure.adapter.in.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
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
    public ResponseEntity<Map<String, String>> getMe(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(Map.of(
            "email", userDetails.getUsername(),
            "role", "ROLE_ADMIN"
        ));
    }
}
