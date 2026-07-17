package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Execution.TestCaseResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class SubmissionResponse {

    private UUID id;

    private UUID assignmentId;

    private String assignmentTitle;

    private UUID studentId;

    private String studentName;

    private String fileName;

    private String downloadUrl;

    /** Lets the frontend decide how to render an inline preview without guessing from the filename. */
    private String contentType;

    private long fileSize;

    private String comment;

    private boolean reviewed;

    private boolean late;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    // ---- Grading ----
    private Double score;

    private Double maxScore;

    private String feedback;

    private LocalDateTime gradedAt;

    private String gradedByName;

    private boolean autoGraded;

    // ---- Code execution (programming assignments) ----
    private boolean canRun; // true if this is a programming assignment with a runnable submission

    /** True if the submission has extractable plain-text content (any assignment type) — gates the "Check for AI" button. */
    private boolean hasReadableText;

    /**
     * The submitted source code, when extractable as plain text. Only populated
     * for lecturer/admin/master views — see SubmissionService.toResponse, which
     * omits this for students so they can't read each other's code via the API.
     */
    private String sourceCode;

    private List<TestCaseResult> lastRunResult;

    private LocalDateTime lastRunAt;

    // ---- AI-likelihood analysis ----
    private Integer aiLikelihoodPercent;

    private String aiAnalysisExplanation;

    private LocalDateTime aiAnalyzedAt;
}
