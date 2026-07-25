package com.fixitpro.aichat.chat;

import java.util.List;

public record ChatResponse(String reply, List<ChatMessageDto> messages) {
}
