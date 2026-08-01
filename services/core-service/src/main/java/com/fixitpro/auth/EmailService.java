package com.fixitpro.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Sends transactional email via Resend's REST API
 * (https://resend.com/docs/api-reference/emails/send-email - stable,
 * well-documented API, chosen to minimize integration risk).
 *
 * Uses JDK's built-in java.net.http.HttpClient rather than adding a new
 * Maven dependency (WebClient/RestTemplate) just for one outbound call.
 *
 * Deliberately fire-and-forget from the caller's perspective: a failure to
 * send email should never break the forgot-password flow's response to the
 * client (which always returns success regardless, to avoid leaking whether
 * an email address is registered) - failures are logged, not thrown.
 */
@Component
@Slf4j
public class EmailService {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.email.resend-api-key:}")
    private String resendApiKey;

    @Value("${app.email.from-address:onboarding@resend.dev}")
    private String fromAddress;

    public boolean isConfigured() {
        return resendApiKey != null && !resendApiKey.isBlank();
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (!isConfigured()) {
            log.warn("RESEND_API_KEY not set - skipping password reset email. Reset link: {}", resetLink);
            return;
        }

        try {
            String html = """
                    <p>You requested a password reset for your FixitPro account.</p>
                    <p><a href="%s">Click here to reset your password</a></p>
                    <p>This link expires in 30 minutes. If you didn't request this, you can safely ignore this email.</p>
                    """.formatted(resetLink);

            Map<String, Object> body = Map.of(
                    "from", "FixitPro <" + fromAddress + ">",
                    "to", new String[]{toEmail},
                    "subject", "Reset your FixitPro password",
                    "html", html
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.resend.com/emails"))
                    .header("Authorization", "Bearer " + resendApiKey)
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Password reset email sent to {}", toEmail);
            } else {
                log.error("Resend API returned {} sending to {}: {}", response.statusCode(), toEmail, response.body());
            }
        } catch (Exception e) {
            // Swallow deliberately - see class-level note. Log with enough
            // detail to debug from Render's log stream without exposing
            // anything to the client.
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }
}
