package com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtistResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String artistName;
    private String slug;
    private String bio;
    private String profilePictureUrl;
    private String bannerUrl;
    private String location;
    private String status;
}
