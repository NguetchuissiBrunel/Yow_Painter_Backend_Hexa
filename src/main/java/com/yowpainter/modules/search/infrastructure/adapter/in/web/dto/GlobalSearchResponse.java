package com.yowpainter.modules.search.infrastructure.adapter.in.web.dto;

import com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto.ArtistResponse;
import com.yowpainter.modules.artwork.infrastructure.adapter.in.web.dto.ArtworkResponse;
import com.yowpainter.modules.event.infrastructure.adapter.in.web.dto.EventResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class GlobalSearchResponse {
    private List<ArtistResponse> artists;
    private List<ArtworkResponse> artworks;
    private List<EventResponse> events;
}
