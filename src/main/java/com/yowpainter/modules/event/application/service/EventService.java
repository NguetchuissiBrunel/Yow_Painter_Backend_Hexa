package com.yowpainter.modules.event.application.service;

import com.yowpainter.modules.artist.domain.model.Artist;
import com.yowpainter.modules.artist.domain.port.out.ArtistRepositoryPort;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.EventCreateRequest;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.EventResponse;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.ReservationResponse;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.TicketResponse;
import com.yowpainter.modules.event.domain.model.*;
import com.yowpainter.modules.event.domain.port.out.EventRepositoryPort;
import com.yowpainter.modules.event.domain.port.out.ReservationRepositoryPort;
import com.yowpainter.modules.event.domain.port.out.TicketRepositoryPort;
import com.yowpainter.modules.auth.domain.model.AppUser;
import com.yowpainter.modules.auth.domain.port.out.AppUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepositoryPort eventRepository;
    private final ReservationRepositoryPort reservationRepository;
    private final ArtistRepositoryPort artistRepository;
    private final AppUserRepositoryPort userRepository;
    private final TicketRepositoryPort ticketRepository;

    @Transactional
    public EventResponse createEvent(String artistEmail, EventCreateRequest request) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        Event event = Event.builder()
                .artistId(artist.getId())
                .name(request.getName())
                .description(request.getDescription())
                .posterUrl(request.getPosterUrl())
                .startDateTime(request.getStartDateTime())
                .endDateTime(request.getEndDateTime())
                .location(request.getLocation())
                .type(request.getType())
                .maxCapacity(request.getMaxCapacity())
                .ticketPrice(request.getTicketPrice())
                .status(EventStatus.PUBLISHED)
                .build();

        return mapToResponse(eventRepository.save(event));
    }

    public List<EventResponse> getUpcomingEvents() {
        LocalDateTime now = LocalDateTime.now();
        return eventRepository.findUpcomingEvents(now).stream()
                .map(this::mapToResponse)
                .sorted(Comparator.comparing(EventResponse::getStartDateTime))
                .collect(Collectors.toList());
    }

    public List<EventResponse> getEventsByArtistId(UUID artistId) {
        return eventRepository.findByArtistId(artistId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EventResponse> getEventsByArtistSlug(String slug) {
        Artist artist = artistRepository.findBySlug(slug).orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));
        return eventRepository.findByArtistId(artist.getId()).stream()
                .filter(e -> e.getStatus() == EventStatus.PUBLISHED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<EventResponse> getMyEvents(String artistEmail) {
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow(() -> new IllegalArgumentException("Artiste non trouve"));
        return eventRepository.findByArtistId(artist.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public EventResponse getEventById(UUID id) {
        return mapToResponse(eventRepository.findById(id).orElseThrow());
    }

    public List<EventResponse> searchEvents(String query) {
        return eventRepository.searchPublicEvents(query).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReservationResponse reserveEvent(UUID eventId, String userEmail) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        AppUser user = userRepository.findByEmail(userEmail).orElseThrow();

        if (!event.hasAvailableSeats()) {
            throw new IllegalStateException("Plus de places disponibles");
        }

        boolean isFree = event.getTicketPrice().compareTo(java.math.BigDecimal.ZERO) == 0;

        Reservation reservation = Reservation.builder()
                .event(event)
                .userId(user.getId())
                .status(isFree ? ReservationStatus.CONFIRMED : ReservationStatus.PENDING)
                .build();

        event.setReservedCount(event.getReservedCount() + 1);
        if (event.getMaxCapacity() > 0 && event.getReservedCount() >= event.getMaxCapacity()) {
            event.setStatus(EventStatus.FULL);
        }

        eventRepository.save(event);
        reservation = reservationRepository.save(reservation);

        if (isFree) {
            Ticket ticket = Ticket.builder()
                    .reservation(reservation)
                    .qrCodeData(UUID.randomUUID().toString())
                    .isScanned(false)
                    .build();
            ticketRepository.save(ticket);
        }

        return mapToReservationResponse(reservation);
    }

    @Transactional
    public void confirmPaidReservation(UUID reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation non trouvée"));

        if (reservation.getStatus() == ReservationStatus.CONFIRMED) {
            return;
        }

        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservationRepository.save(reservation);

        Ticket ticket = Ticket.builder()
                .reservation(reservation)
                .qrCodeData(UUID.randomUUID().toString())
                .isScanned(false)
                .build();
        ticketRepository.save(ticket);
    }

    public List<ReservationResponse> getEventReservations(UUID eventId, String artistEmail) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        if (!event.getArtistId().equals(artist.getId())) throw new IllegalArgumentException("Non autorise");

        return reservationRepository.findByEventId(eventId).stream()
                .map(this::mapToReservationResponse)
                .collect(Collectors.toList());
    }

    public ReservationResponse getReservationById(UUID reservationId) {
        Reservation res = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("Reservation non trouvée"));
        return mapToReservationResponse(res);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, String artistEmail, EventCreateRequest request) {
        Event event = eventRepository.findById(id).orElseThrow();
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        if (!event.getArtistId().equals(artist.getId())) throw new IllegalArgumentException("Non autorise");

        event.setName(request.getName());
        event.setDescription(request.getDescription());
        event.setPosterUrl(request.getPosterUrl());
        event.setStartDateTime(request.getStartDateTime());
        event.setEndDateTime(request.getEndDateTime());
        event.setLocation(request.getLocation());
        event.setMaxCapacity(request.getMaxCapacity());
        event.setTicketPrice(request.getTicketPrice());

        return mapToResponse(eventRepository.save(event));
    }

    @Transactional
    public void cancelEvent(UUID id, String artistEmail) {
        Event event = eventRepository.findById(id).orElseThrow();
        Artist artist = artistRepository.findByEmail(artistEmail).orElseThrow();
        if (!event.getArtistId().equals(artist.getId())) throw new IllegalArgumentException("Non autorise");

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
    }

    @Transactional
    public TicketResponse validateTicket(String qrCodeData) {
        Ticket ticket = ticketRepository.findByQrCodeData(qrCodeData)
                .orElseThrow(() -> new IllegalArgumentException("Billet invalide"));
        
        if (ticket.isScanned()) {
            throw new IllegalStateException("Ce billet a déjà été scanné le " + ticket.getScannedAt());
        }

        ticket.setScanned(true);
        ticket.setScannedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        return mapToTicketResponse(ticket);
    }

    @Transactional
    public void cancelAbandonedReservations() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        List<Reservation> abandoned = reservationRepository.findByStatusAndReservedAtBefore(ReservationStatus.PENDING, threshold);

        for (Reservation res : abandoned) {
            res.setStatus(ReservationStatus.CANCELLED);
            reservationRepository.save(res);

            Event event = res.getEvent();
            event.setReservedCount(event.getReservedCount() - 1);
            if (event.getStatus() == EventStatus.FULL) {
                event.setStatus(EventStatus.PUBLISHED);
            }
            eventRepository.save(event);

            log.info("Cancelled abandoned reservation: {} for event: {}", res.getId(), event.getName());
        }
    }

    public List<String> getLocations() {
        return eventRepository.findDistinctLocations();
    }

    private EventResponse mapToResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .artistId(event.getArtistId())
                .name(event.getName())
                .description(event.getDescription())
                .posterUrl(com.yowpainter.shared.utils.UrlSanitizer.sanitizeFileUrl(event.getPosterUrl()))
                .startDateTime(event.getStartDateTime())
                .endDateTime(event.getEndDateTime())
                .location(event.getLocation())
                .type(event.getType())
                .maxCapacity(event.getMaxCapacity())
                .reservedCount(event.getReservedCount())
                .ticketPrice(event.getTicketPrice())
                .status(event.getStatus())
                .build();
    }

    private ReservationResponse mapToReservationResponse(Reservation res) {
        return ReservationResponse.builder()
                .id(res.getId())
                .eventId(res.getEvent().getId())
                .eventName(res.getEvent().getName())
                .userId(res.getUserId())
                .status(res.getStatus())
                .createdAt(res.getReservedAt())
                .build();
    }

    private TicketResponse mapToTicketResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .reservationId(ticket.getReservation().getId())
                .isScanned(ticket.isScanned())
                .scannedAt(ticket.getScannedAt())
                .build();
    }
}
