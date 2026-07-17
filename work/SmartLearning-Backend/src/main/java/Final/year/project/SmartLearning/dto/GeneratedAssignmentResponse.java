package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * A draft produced by the AI. Nothing here is persisted automatically — the
 * lecturer reviews and edits it in the assignment form, then submits it through
 * the normal CreateAssignmentRequest flow like any other assignment.
 */
@Data
@Builder
public class GeneratedAssignmentResponse {

    private String title;

    private String description;

    private Double suggestedMaxScore;

    /** Only populated for PROGRAMMING assignments. */
    private List<TestCaseDto> testCases;

    /** A short note disclosing that this draft was AI-generated and needs review. */
    private String disclaimer;
}
