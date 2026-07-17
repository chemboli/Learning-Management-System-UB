package Final.year.project.SmartLearning.Ai;

import Final.year.project.SmartLearning.dto.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Wraps the two AI-assisted features in this app:
 *
 *  1. Assignment generation: turns a lecturer's short prompt ("we covered X today")
 *     into a draft title/description/test-cases that the lecturer reviews and edits
 *     before it's saved as a real assignment. The AI never publishes anything itself.
 *
 *  2. AI-likelihood analysis of a submission: gives the lecturer a 0-100 estimate of
 *     how "AI-written" a piece of text reads, with a short explanation. This is a
 *     TRIAGE SIGNAL, not proof of misconduct — see the disclaimer baked into every
 *     response. No automated AI-text detector (including this one) can reliably
 *     prove authorship; false positives on genuine student writing are common
 *     industry-wide. This must never auto-fail or auto-penalize a student — it only
 *     surfaces a number + explanation for a human to investigate further.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private static final String ANALYSIS_DISCLAIMER =
            "This is an automated estimate, not proof. AI-detection tools can be wrong in " +
            "both directions — genuine student writing is sometimes flagged, and AI-assisted " +
            "writing can go undetected. Use this as a prompt to look closer, not as grounds " +
            "for an accusation on its own.";

    private static final String GENERATION_DISCLAIMER =
            "AI-generated draft — review and edit before publishing. Verify all test cases " +
            "produce the output you expect.";

    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean isConfigured() {
        return geminiClient.isConfigured();
    }

    // ---- Feature 1: Assignment generation --------------------------------

    public GeneratedAssignmentResponse generateAssignment(GenerateAssignmentRequest request) {

        boolean programming = "PROGRAMMING".equalsIgnoreCase(request.getAssignmentType());
        int testCaseCount = request.getTestCaseCount() != null ? request.getTestCaseCount() : 4;

        String systemPrompt = buildGenerationSystemPrompt(programming, request.getProgrammingLanguage(), testCaseCount);

        String userMessage = "Lecturer's note: " + safe(request.getPrompt());

        String raw = geminiClient.complete(systemPrompt, userMessage);
        JsonNode json = extractJson(raw);

        GeneratedAssignmentResponse.GeneratedAssignmentResponseBuilder builder =
                GeneratedAssignmentResponse.builder()
                        .title(json.path("title").asText("Untitled assignment"))
                        .description(json.path("description").asText(""))
                        .suggestedMaxScore(json.path("suggestedMaxScore").asDouble(100))
                        .disclaimer(GENERATION_DISCLAIMER);

        if (programming) {
            List<TestCaseDto> testCases = new ArrayList<>();
            int seq = 0;
            for (JsonNode tcNode : json.path("testCases")) {
                seq++;
                testCases.add(TestCaseDto.builder()
                        .sequence(seq)
                        .label(tcNode.path("label").asText("Test " + seq))
                        .input(tcNode.path("input").asText(""))
                        .expectedOutput(tcNode.path("expectedOutput").asText(""))
                        .weight(tcNode.path("weight").asDouble(1.0))
                        .hidden(tcNode.path("hidden").asBoolean(false))
                        .build());
            }
            builder.testCases(testCases);
        }

        return builder.build();
    }

    private String buildGenerationSystemPrompt(boolean programming, String language, int testCaseCount) {

        StringBuilder sb = new StringBuilder();
        sb.append("You are helping a university lecturer draft a new course assignment from a short note ")
          .append("about what was covered in class. Respond with ONLY a single JSON object — no prose, ")
          .append("no markdown code fences — matching this shape:\n\n");

        if (programming) {
            sb.append("{\n")
              .append("  \"title\": string,\n")
              .append("  \"description\": string (clear instructions for students, 2-5 sentences),\n")
              .append("  \"suggestedMaxScore\": number,\n")
              .append("  \"testCases\": [\n")
              .append("    { \"label\": string, \"input\": string, \"expectedOutput\": string, ")
              .append("\"weight\": number, \"hidden\": boolean }\n")
              .append("  ]\n")
              .append("}\n\n")
              .append("The assignment is a PROGRAMMING assignment in ").append(language).append(". ")
              .append("Generate exactly ").append(testCaseCount).append(" test cases covering normal cases, ")
              .append("at least one edge case, ordered easiest first. \"input\" is exactly what gets piped to ")
              .append("the program's stdin; \"expectedOutput\" is exactly what the program should print to ")
              .append("stdout for that input (no extra commentary). Mark roughly half the test cases as ")
              .append("hidden (true) so students can see some sample cases but not all of them. Weights should ")
              .append("sum to suggestedMaxScore.");
        } else {
            sb.append("{\n")
              .append("  \"title\": string,\n")
              .append("  \"description\": string (clear instructions for students, 3-6 sentences),\n")
              .append("  \"suggestedMaxScore\": number\n")
              .append("}\n\n")
              .append("This is a REGULAR (file-upload, manually graded) assignment — do not include test cases.");
        }

        sb.append("\n\nBase the assignment on the lecturer's note about what was covered. If the note is vague, ")
          .append("make reasonable assumptions appropriate for a university-level course and keep scope modest ")
          .append("(something doable in a few hours).");

        return sb.toString();
    }

    // ---- Feature 2: AI-likelihood analysis on a submission ----------------

    public AiAnalysisResponse analyzeSubmissionText(String text) {

        if (text == null || text.isBlank()) {
            return AiAnalysisResponse.builder()
                    .aiLikelihoodPercent(0)
                    .explanation("No readable text content was found in this submission to analyze.")
                    .disclaimer(ANALYSIS_DISCLAIMER)
                    .analyzedAt(LocalDateTime.now())
                    .build();
        }

        String systemPrompt =
                "You assist a university lecturer in spotting writing that may be AI-generated, as a triage " +
                "signal only — never a verdict. Analyze the student text provided and respond with ONLY a single " +
                "JSON object, no prose, no markdown fences:\n\n" +
                "{\n" +
                "  \"aiLikelihoodPercent\": integer 0-100,\n" +
                "  \"explanation\": string (2-4 sentences, plain language, citing specific patterns you noticed " +
                "such as unusually uniform sentence structure, generic phrasing, lack of personal voice, or " +
                "conversely natural inconsistencies/typos/personal voice that suggest human authorship)\n" +
                "}\n\n" +
                "Be appropriately uncertain — do not claim certainty either way. Short or code-only submissions " +
                "should get a lower-confidence, more cautious explanation.";

        // Cap how much text we send — both for cost and because extremely long
        // submissions don't need the whole thing to get a useful signal.
        String truncated = text.length() > 8000 ? text.substring(0, 8000) + "\n...[truncated]" : text;

        String raw = geminiClient.complete(systemPrompt, truncated);
        JsonNode json = extractJson(raw);

        int percent = clamp(json.path("aiLikelihoodPercent").asInt(50), 0, 100);
        String explanation = json.path("explanation").asText("No explanation was provided.");

        return AiAnalysisResponse.builder()
                .aiLikelihoodPercent(percent)
                .explanation(explanation)
                .disclaimer(ANALYSIS_DISCLAIMER)
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    // ---- helpers -----------------------------------------------------------

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "(no details provided)" : s.trim();
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Claude is instructed to return raw JSON, but models occasionally wrap output in
     * markdown fences or add stray text — this pulls out the first {...} block defensively.
     */
    private JsonNode extractJson(String raw) {
        try {
            return objectMapper.readTree(raw);
        } catch (Exception ignored) {
            // fall through to bracket-extraction below
        }

        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');

        if (start >= 0 && end > start) {
            try {
                return objectMapper.readTree(raw.substring(start, end + 1));
            } catch (Exception e) {
                log.warn("Could not parse AI response as JSON: {}", raw);
            }
        }

        throw new AiUnavailableException("The AI provider returned a response that could not be understood.");
    }
}
