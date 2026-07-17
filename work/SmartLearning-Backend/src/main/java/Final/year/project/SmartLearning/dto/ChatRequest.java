package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {

    private String message;

    /** Prior turns in this conversation, oldest first, for multi-turn context. */
    private List<ChatTurn> history;

    @Data
    public static class ChatTurn {
        private String role; // "user" | "assistant"
        private String content;
    }
}
