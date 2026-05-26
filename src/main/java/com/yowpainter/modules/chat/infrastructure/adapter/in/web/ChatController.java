package com.yowpainter.modules.chat.infrastructure.adapter.in.web;

import com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto.ChatMessageDto;
import com.yowpainter.modules.chat.application.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;

    @MessageMapping("/chat")
    public void processMessage(@Payload ChatMessageDto chatMessageDto) {
        try {
            ChatMessageDto savedMessage = chatMessageService.save(chatMessageDto);
            
            // On récupère l'email du destinataire pour envoyer au bon "User" STOMP
            String recipientEmail = chatMessageService.getRecipientEmail(chatMessageDto.getRecipientId());

            System.out.println("CHAT DEBUG: Dispatching message via WebSocket to " + recipientEmail);

            messagingTemplate.convertAndSendToUser(
                    recipientEmail,
                    "/queue/messages",
                    savedMessage
            );
        } catch (Exception e) {
            System.err.println("CHAT ERROR: Failed to process STOMP message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @GetMapping("/messages/{senderId}/{recipientId}")
    public ResponseEntity<List<ChatMessageDto>> findChatMessages(
            @PathVariable UUID senderId,
            @PathVariable UUID recipientId) {
        return ResponseEntity.ok(chatMessageService.findChatMessages(senderId, recipientId));
    }

    @GetMapping("/chat/contacts/{userId}")
    public ResponseEntity<List<com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto.UserChatDto>> getContacts(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatMessageService.getRecentContacts(userId));
    }

    @GetMapping("/chat/search")
    public ResponseEntity<List<com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto.UserChatDto>> searchUsers(@RequestParam String q) {
        return ResponseEntity.ok(chatMessageService.searchUsers(q));
    }

    @GetMapping("/chat/suggestions/{userId}")
    public ResponseEntity<List<com.yowpainter.modules.chat.infrastructure.adapter.in.web.dto.UserChatDto>> getSuggestions(@PathVariable UUID userId) {
        return ResponseEntity.ok(chatMessageService.getSuggestedContacts(userId));
    }

    @PostMapping("/chat/read/{recipientId}/{senderId}")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID recipientId, @PathVariable UUID senderId) {
        chatMessageService.markMessagesAsRead(recipientId, senderId);
        return ResponseEntity.ok().build();
    }
}
