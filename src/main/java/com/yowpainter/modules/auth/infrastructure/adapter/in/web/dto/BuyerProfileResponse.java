package com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BuyerProfileResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String profilePictureUrl;
    private String bio;
    private String role;
}
