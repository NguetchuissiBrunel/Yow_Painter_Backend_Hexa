package com.yowpainter.modules.artist.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ArtistUpdateRequest {
    private String firstName;
    private String lastName;
    @NotBlank
    private String artistName;
    private String bio;
    private String profilePictureUrl;
    private String bannerUrl;
    private String location;
}
