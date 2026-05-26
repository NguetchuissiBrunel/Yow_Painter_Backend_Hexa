package com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ArtistAnalyticsResponse {
    private long totalArtworks;
    private long publishedArtworks;
    private long totalLikes;
    private long totalSales;
    private double totalRevenue;
    private long upcomingEvents;
}
