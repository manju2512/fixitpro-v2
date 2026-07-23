package com.fixitpro.aichat;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Placeholder endpoint confirming the service boots and is reachable.
 * Real chat endpoints (/api/chat, streaming, tool-use against core-service)
 * are built in Phase 5.
 */
@RestController
public class StatusController {

    @GetMapping("/api/chat/status")
    public Map<String, String> status() {
        return Map.of(
                "service", "ai-chat-service",
                "status", "scaffolded",
                "note", "Chat endpoints land in Phase 5"
        );
    }
}
