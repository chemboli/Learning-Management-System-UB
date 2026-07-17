package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class AssignmentResponse {

    private UUID id;

    private String title;

    private String description;

    private LocalDateTime dueDate;

    private boolean published;

    private boolean overdue;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private String lecturerName;

    private LocalDateTime createdAt;

    private String assignmentType; // "REGULAR" | "PROGRAMMING"

    private String programmingLanguage; // "JAVA" | "PYTHON" | "C" | "CPP", null for REGULAR

    private double maxScore;

    private double latePenaltyPercent;

    /** Visible to everyone for PROGRAMMING assignments — students should know the sample cases. */
    private List<TestCaseDto> testCases;

    // Populated for the current student only, null/omitted for lecturer/admin views
    private String mySubmissionStatus; // "NOT_SUBMITTED" | "SUBMITTED" | "REVIEWED"

    private Integer submissionCount; // populated for lecturer/admin views

    /** Average score across all graded submissions for this assignment (lecturer/admin views only). */
    private Double averageScore;
}
