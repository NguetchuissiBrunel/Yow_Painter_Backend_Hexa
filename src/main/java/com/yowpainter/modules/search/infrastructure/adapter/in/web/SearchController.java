package com.yowpainter.modules.search.infrastructure.adapter.in.web;

import com.yowpainter.modules.artist.application.service.ArtistService;
import com.yowpainter.modules.artwork.application.service.ArtworkService;
import com.yowpainter.modules.event.application.service.EventService;
import com.yowpainter.modules.search.infrastructure.adapter.in.web.dto.GlobalSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/search")
@RequiredArgsConstructor
@Tag(name = "Global Search", description = "Recherche unifiee sur toute la plateforme")
public class SearchController {

    private final ArtistService artistService;
    private final ArtworkService artworkService;
    private final EventService eventService;

    @GetMapping("/global")
    @Operation(summary = "Recherche globale (Artistes + Oeuvres + Evenements)")
    public ResponseEntity<GlobalSearchResponse> globalSearch(@RequestParam String q) {
        return ResponseEntity.ok(GlobalSearchResponse.builder()
                .artists(artistService.searchArtists(q))
                .artworks(artworkService.searchArtworks(q))
                .events(eventService.searchEvents(q))
                .build());
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Suggestions d'autocompletion")
    public ResponseEntity<List<String>> getSuggestions(@RequestParam String q) {
        return ResponseEntity.ok(artworkService.getSuggestions(q));
    }
}
