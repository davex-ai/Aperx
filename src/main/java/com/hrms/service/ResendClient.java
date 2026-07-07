package com.hrms.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class ResendClient {

    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    @Value("${app.resend.api-key}")
    private String apiKey;

    @Value("${app.mail.from}")
    private String fromAddress;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void send(String toEmail, String subject, String htmlBody) {
        try {
            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", List.of(toEmail),
                    "subject", subject,
                    "html", htmlBody
            );

            String jsonBody = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(RESEND_API_URL))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new EmailDeliveryException(
                        "Resend API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (EmailDeliveryException e) {
            throw e;
        } catch (Exception e) {
            throw new EmailDeliveryException("Failed to reach Resend API: " + e.getMessage(), e);
        }
    }

    public static class EmailDeliveryException extends RuntimeException {
        public EmailDeliveryException(String message) {
            super(message);
        }

        public EmailDeliveryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
