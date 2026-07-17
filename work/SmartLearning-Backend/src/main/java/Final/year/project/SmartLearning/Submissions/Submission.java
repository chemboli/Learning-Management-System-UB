package Final.year.project.SmartLearning.Submissions;

import Final.year.project.SmartLearning.Assignments.Assignment;
import Final.year.project.SmartLearning.Users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String fileName;

    private String objectName; // MinIO file path

    private String contentType;

    private long fileSize;

    @Column(length = 2000)
    private String comment; // optional note from the student

    private boolean reviewed;

    private LocalDateTime submittedAt;

    private LocalDateTime reviewedAt;

    // ---- Grading -------------------------------------------------------

    /** Score awarded, out of assignment.maxScore. Null until graded. */
    private Double score;

    @Column(length = 3000)
    private String feedback;

    private LocalDateTime gradedAt;

    @ManyToOne
    @JoinColumn(name = "graded_by")
    private User gradedBy;

    /** Whether the current score came from automated test execution. */
    private boolean autoGraded;

    // ---- Automated code execution (programming assignments) -----------

    /** Most recent raw source code submitted, extracted for execution if the upload is a code file. */
    @Column(length = 20000)
    private String sourceCode;

    /** JSON-serialized list of per-test-case results from the last run. */
    @Column(length = 20000)
    private String lastRunResult;

    private LocalDateTime lastRunAt;

    // ---- AI-likelihood analysis ------------------------------------------

    /** 0-100, null until analyzed. Triage signal only — never a verdict, see AiService. */
    private Integer aiLikelihoodPercent;

    @Column(length = 2000)
    private String aiAnalysisExplanation;

    private LocalDateTime aiAnalyzedAt;

    @ManyToOne
    private Assignment assignment;

    @ManyToOne
    private User student;
}
