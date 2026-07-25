package com.fixitpro.aichat.chat;

import com.fixitpro.aichat.auth.AuthWebFilter;
import com.fixitpro.aichat.auth.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/api/chat/message")
    public Mono<ChatResponse> sendMessage(@Valid @RequestBody ChatRequest request, ServerWebExchange exchange) {
        // AuthWebFilter already rejected the request with 401 if these are missing, so they're
        // guaranteed present here.
        AuthenticatedUser user = exchange.getAttribute(AuthWebFilter.USER_ATTRIBUTE);
        String token = exchange.getAttribute(AuthWebFilter.TOKEN_ATTRIBUTE);
        return chatService.handle(user, token, request);
    }
}
