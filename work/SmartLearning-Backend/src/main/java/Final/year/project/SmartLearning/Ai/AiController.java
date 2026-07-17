package Final.year.project.SmartLearning.Ai;

import Final.year.project.SmartLearning.dto.GenerateAssignmentRequest;
import Final.year.project.SmartLearning.dto.GeneratedAssignmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    /** Whether AI features are usable right now (an API key is configured server-side). */
    @GetMapping("/status")
    public AiStatusResponse status() {
        return new AiStatusResponse(aiService.isConfigured());
    }

    /**
     * Drafts an assignment (title, description, and for programming assignments,
     * test cases) from a short lecturer prompt. Returns a draft only — nothing is
     * saved until the lecturer reviews it and submits the normal assignment form.
     */
    @PostMapping("/generate-assignment")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public GeneratedAssignmentResponse generateAssignment(@RequestBody GenerateAssignmentRequest request) {
        return aiService.generateAssignment(request);
    }

    public record AiStatusResponse(boolean configured) {}
}
