package com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserChatDto {
    private UUID id;
    private String name; // Concatenation or artistName
    private String profilePictureUrl;
    private String role;
    private String lastMessage;
    private int unreadCount;
}
