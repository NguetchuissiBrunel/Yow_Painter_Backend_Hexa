package com.yowpainter.modules.event.infrastructure.adapter.in.web;

import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.EventCreateRequest;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.EventResponse;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.ReservationResponse;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.TicketResponse;
import com.yowpainter.modules.event.application.service.EventService;
import com.yowpainter.modules.payment.application.service.PaymentService;
import com.yowpainter.shared.tenant.TenantIdentifierResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Gestion complete des evenements et reservations")
public class EventController {

    private final EventService eventService;
    private final PaymentService paymentService;
    private final TenantIdentifierResolver tenantResolver;

    @GetMapping("/public/events")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Lister les evenements a venir")
    public ResponseEntity<List<EventResponse>> getUpcomingEvents() {
        return ResponseEntity.ok(eventService.getUpcomingEvents());
    }

    @GetMapping("/public/artists/{artistId}/events")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Lister les événements d'un artiste spécifique par ID")
    public ResponseEntity<List<EventResponse>> getEventsByArtist(@PathVariable UUID artistId) {
        return ResponseEntity.ok(eventService.getEventsByArtistId(artistId));
    }

    @GetMapping("/v1/public/{artistSlug}/events")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Lister les événements d'un artiste spécifique par slug")
    public ResponseEntity<List<EventResponse>> getEventsByArtistSlug(@PathVariable String artistSlug) {
        return ResponseEntity.ok(eventService.getEventsByArtistSlug(artistSlug));
    }

    @GetMapping("/public/events/{id}")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Voir les details d'un evenement")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/public/events/search")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Rechercher des evenements par nom ou lieu")
    public ResponseEntity<List<EventResponse>> searchEvents(@RequestParam String q) {
        return ResponseEntity.ok(eventService.searchEvents(q));
    }

    @PostMapping("/events")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Creer un evenement (Artiste)")
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(userDetails.getUsername(), request));
    }

    @GetMapping("/events/me")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Lister mes événements (Artiste - Dashboard)")
    public ResponseEntity<List<EventResponse>> getMyEvents(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getMyEvents(userDetails.getUsername()));
    }

    @PutMapping("/events/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Modifier un evenement (Artiste proprietaire)")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody EventCreateRequest request) {
        return ResponseEntity.ok(eventService.updateEvent(id, userDetails.getUsername(), request));
    }

    @DeleteMapping("/events/{id}")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Annuler/Supprimer un evenement")
    public ResponseEntity<Void> cancelEvent(@PathVariable UUID id, @AuthenticationPrincipal UserDetails userDetails) {
        eventService.cancelEvent(id, userDetails.getUsername());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/events/{eventId}/reservations")
    @PreAuthorize("hasAnyRole('BUYER', 'ARTIST')")
    @Operation(summary = "RESERVER une place")
    public ResponseEntity<ReservationResponse> reserveEvent(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.reserveEvent(eventId, userDetails.getUsername()));
    }

    @PostMapping("/events/reservations/{id}/checkout")
    @PreAuthorize("hasAnyRole('BUYER', 'ARTIST')")
    @Operation(summary = "Initier le paiement Mobile Money (MOMO/Orange) pour une réservation")
    public ResponseEntity<Map<String, String>> checkoutReservation(
            @PathVariable UUID id,
            @RequestParam String phoneNumber,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        ReservationResponse reservation = eventService.getReservationById(id);
        EventResponse event = eventService.getEventById(reservation.getEventId());
        
        if (event.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cet événement est gratuit"));
        }
        
        String tenantId = tenantResolver.resolveCurrentTenantIdentifier();
        
        String paymentReference = paymentService.initiateMobileMoneyPayment(
                id, 
                "RESERVATION", 
                event.getTicketPrice(), 
                tenantId, 
                userDetails.getUsername(),
                phoneNumber
        );
        
        return ResponseEntity.ok(Map.of("paymentReference", paymentReference));
    }

    @GetMapping("/events/{id}/reservations")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Lister les inscrits (Artiste proprietaire)")
    public ResponseEntity<List<ReservationResponse>> getReservations(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(eventService.getEventReservations(id, userDetails.getUsername()));
    }

    @PostMapping("/events/tickets/validate")
    @PreAuthorize("hasRole('ARTIST')")
    @Operation(summary = "Valider un billet par QR Code")
    public ResponseEntity<TicketResponse> validateTicket(@RequestParam String qrCodeData) {
        return ResponseEntity.ok(eventService.validateTicket(qrCodeData));
    }

    @GetMapping("/public/events/metadata/locations")
    @PreAuthorize("permitAll()")
    @Operation(summary = "Lister les lieux disponibles pour les filtres")
    public ResponseEntity<List<String>> getLocations() {
        return ResponseEntity.ok(eventService.getLocations());
    }
}
