package Final.year.project.SmartLearning.Chat;

import Final.year.project.SmartLearning.Ai.GeminiClient;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.dto.ChatRequest;
import Final.year.project.SmartLearning.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Powers the in-app AI chat assistant. Every reply is grounded in the
 * requesting user's own real data (see AcademicContextBuilder) — students get
 * advice based on their actual enrolled courses and submission history;
 * lecturers get advice based only on the courses they personally teach.
 * Neither role can see another user's data through this chat; the scoping
 * happens server-side before anything is sent to the AI provider.
 */
@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_HISTORY_TURNS = 10;

    private final GeminiClient geminiClient;
    private final AcademicContextBuilder contextBuilder;

    public boolean isConfigured() {
        return geminiClient.isConfigured();
    }

    public ChatResponse chat(User user, ChatRequest request) {

        String academicContext = contextBuilder.buildContext(user);

        String systemPrompt = buildSystemPrompt(user, academicContext);

        List<GeminiClient.Turn> turns = new ArrayList<>();

        if (request.getHistory() != null) {
            // Cap how much history we resend — keeps requests small and cheap,
            // and a long-running chat doesn't need the entire transcript to
            // stay coherent for a few turns back.
            int start = Math.max(0, request.getHistory().size() - MAX_HISTORY_TURNS);
            for (ChatRequest.ChatTurn turn : request.getHistory().subList(start, request.getHistory().size())) {
                String geminiRole = "assistant".equalsIgnoreCase(turn.getRole()) ? "model" : "user";
                turns.add(new GeminiClient.Turn(geminiRole, turn.getContent()));
            }
        }

        turns.add(new GeminiClient.Turn("user", request.getMessage()));

        String reply = geminiClient.completeConversation(systemPrompt, turns);

        return ChatResponse.builder().reply(reply).build();
    }

    private String buildSystemPrompt(User user, String academicContext) {

        String role = user.getRole().name();
        String roleGuidance = switch (role) {
            case "STUDENT" -> """
                    You are talking to a STUDENT. Use their enrollment and submission data below to give
                    specific, actionable recommendations — which assignments need attention, which courses
                    they're falling behind in, what to prioritize, and concrete study suggestions tied to
                    their actual scores and feedback. Don't give generic study tips disconnected from their
                    real data. If they ask about something outside their own courses, answer helpfully but
                    note you can only see their own enrolled courses.
                    """;
            case "LECTURER" -> """
                    You are talking to a LECTURER. Use the data below — which is limited to courses they
                    personally teach — to help them spot patterns: assignments with low average scores,
                    students who haven't submitted, ungraded work piling up, or test cases many students
                    are failing. Give specific, actionable suggestions, not generic teaching advice. If they
                    ask about a course they don't teach, note you can only see courses assigned to them.
                    """;
            default -> "Answer helpfully. No course-specific data applies to this user's role.";
        };

        return """
                You are the AI assistant built into SmartLearning, a university learning management system.
                Be concise, warm, and practical. Use plain text (no markdown headers or heavy formatting) —
                this is a chat window, not a document. Keep replies focused; a few short paragraphs or a
                short list is usually enough.

                %s

                Here is this user's real, current data from the system (already scoped to only what they
                are allowed to see — never claim to know about courses, students, or data outside this):

                %s
                """.formatted(roleGuidance, academicContext);
    }
}
