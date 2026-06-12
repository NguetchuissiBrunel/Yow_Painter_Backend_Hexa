package com.yowpainter.modules.notification.infrastructure.adapter.in.web;

import com.yowpainter.modules.notification.application.service.NotificationService;
import com.yowpainter.modules.notification.domain.model.Notification;
import com.yowpainter.shared.security.AuthenticatedUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notifications systeme et alertes pour l'utilisateur")
public class NotificationController {

    private final NotificationService notificationService;
    private final AuthenticatedUserResolver authenticatedUserResolver;

    @GetMapping
    @Operation(summary = "Lister mes notifications")
    public ResponseEntity<List<Notification>> getMyNotifications(Authentication authentication) {
        String email = authenticatedUserResolver.requireEmail(authentication);
        return ResponseEntity.ok(notificationService.getNotificationsForUser(email));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Marquer une notification comme lue")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/mark-all-read")
    @Operation(summary = "Tout marquer comme lu")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        String email = authenticatedUserResolver.requireEmail(authentication);
        notificationService.markAllAsRead(email);
        return ResponseEntity.ok().build();
    }
}
