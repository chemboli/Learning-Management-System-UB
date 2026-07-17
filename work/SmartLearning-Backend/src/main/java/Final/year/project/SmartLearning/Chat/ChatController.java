package Final.year.project.SmartLearning.Chat;

import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.ChatRequest;
import Final.year.project.SmartLearning.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final UserRepository userRepository;

    @GetMapping("/status")
    public StatusResponse status() {
        return new StatusResponse(chatService.isConfigured());
    }

    /** Available to every logged-in role — students, lecturers, admins, master. */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ChatResponse chat(@RequestBody ChatRequest request) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return chatService.chat(user, request);
    }

    public record StatusResponse(boolean configured) {}
}
