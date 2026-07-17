package Final.year.project.SmartLearning.Ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Minimal wrapper around Google's Gemini API
 * (https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent).
 *
 * Used instead of Anthropic's API because Gemini has a genuinely free tier —
 * no credit card, no expiration — which fits a student project with no API
 * budget. Quality is a step below Claude/GPT-4-class models, but it's more
 * than sufficient for short, structured tasks like drafting an assignment or
 * giving a 0-100 AI-likelihood estimate.
 *
 * Note: on the free tier, Google's terms allow prompts/responses to be used
 * to improve their models (this is disclosed in the README / setup notes —
 * worth knowing if this is ever pointed at sensitive data).
 */
@Slf4j
@Component
public class GeminiClient {

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.max-tokens:1500}")
    private int maxTokens;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Sends a single-turn prompt to Gemini and returns the plain-text reply.
     * Throws AiUnavailableException if the API key isn't configured or the call fails,
     * so callers can degrade gracefully instead of crashing a request.
     */
    public String complete(String systemPrompt, String userMessage) {
        return completeConversation(systemPrompt, List.of(new Turn("user", userMessage)));
    }

    /** One turn in a conversation sent to Gemini. role is "user" or "model". */
    public record Turn(String role, String text) {}

    /**
     * Same as complete(), but supports multi-turn conversation history —
     * used by the chat assistant so it can refer back to earlier messages
     * in the same session.
     *
     * Retries automatically on 503 (model temporarily overloaded — common on
     * the free tier during peak hours) and 429 (rate limited), with a short
     * backoff between attempts. Other errors fail immediately since retrying
     * won't help (bad request, auth failure, etc.).
     */
    public String completeConversation(String systemPrompt, List<Turn> turns) {

        if (!isConfigured()) {
            throw new AiUnavailableException(
                    "The AI provider is not configured. Set gemini.api-key (or the " +
                    "GEMINI_API_KEY environment variable) to enable AI features."
            );
        }

        final int maxAttempts = 3;
        RuntimeException lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return attemptCompletion(systemPrompt, turns);
            } catch (RetryableAiException e) {
                lastFailure = e;
                if (attempt < maxAttempts) {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        // All retries exhausted — surface a clearer message than the raw status code.
        String reason = lastFailure != null ? lastFailure.getMessage() : "unknown error";
        throw new AiUnavailableException(
                "The AI provider is temporarily overloaded and didn't respond after " + maxAttempts +
                " attempts (" + reason + "). This is common on the free tier during busy periods — " +
                "please try again in a minute."
        );
    }

    /** Thrown internally for failures worth retrying (503/429); never escapes completeConversation(). */
    private static class RetryableAiException extends RuntimeException {
        RetryableAiException(String message) {
            super(message);
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            // 800ms, then 1600ms — short enough to still feel responsive in a chat UI.
            Thread.sleep(800L * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private String attemptCompletion(String systemPrompt, List<Turn> turns) {
        try {
            List<Map<String, Object>> contents = turns.stream()
                    .map(t -> Map.<String, Object>of(
                            "role", t.role(),
                            "parts", List.of(Map.of("text", t.text()))
                    ))
                    .toList();

            Map<String, Object> body = Map.of(
                    "systemInstruction", Map.of(
                            "parts", List.of(Map.of("text", systemPrompt))
                    ),
                    "contents", contents,
                    "generationConfig", Map.of(
                            "maxOutputTokens", maxTokens
                    )
            );

            String json = objectMapper.writeValueAsString(body);

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-goog-api-key", apiKey)
                    .timeout(Duration.ofSeconds(45))
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("Gemini API returned {}: {}", response.statusCode(), truncateForLog(response.body()));

                if (response.statusCode() == 503) {
                    throw new RetryableAiException("Model temporarily overloaded (503)");
                }

                if (response.statusCode() == 429) {
                    throw new RetryableAiException("Rate limited (429)");
                }

                throw new AiUnavailableException(
                        "The AI provider returned an error (status " + response.statusCode() + "). " +
                        "Please try again shortly."
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");

            if (!candidates.isArray() || candidates.isEmpty()) {
                // Often means the prompt or response was blocked by Gemini's safety
                // filters rather than a hard error — surface something useful.
                String blockReason = root.path("promptFeedback").path("blockReason").asText(null);
                if (blockReason != null) {
                    throw new AiUnavailableException(
                            "The AI provider declined to respond to this request (reason: " + blockReason + ")."
                    );
                }
                throw new AiUnavailableException("The AI provider returned an empty response.");
            }

            StringBuilder text = new StringBuilder();
            for (JsonNode part : candidates.get(0).path("content").path("parts")) {
                if (part.has("text")) {
                    text.append(part.path("text").asText());
                }
            }

            if (text.length() == 0) {
                throw new AiUnavailableException("The AI provider returned an empty response.");
            }

            return text.toString();

        } catch (RetryableAiException e) {
            throw e;
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to call Gemini API", e);
            throw new AiUnavailableException("Could not reach the AI provider: " + e.getMessage());
        }
    }

    private String truncateForLog(String s) {
        if (s == null) return "";
        return s.length() > 500 ? s.substring(0, 500) + "..." : s;
    }
}
