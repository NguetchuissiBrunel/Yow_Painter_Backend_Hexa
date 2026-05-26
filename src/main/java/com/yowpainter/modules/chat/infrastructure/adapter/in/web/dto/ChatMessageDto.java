package com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto;

import com.yowpainter.modules.chat.domain.model.ChatMessageStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ChatMessageDto {
    private UUID id;
    private String chatId;
    private UUID senderId;
    private UUID recipientId;
    private String content;
    private LocalDateTime timestamp;
    private ChatMessageStatus status;
}
