package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class CreateAssignmentRequest {

    private UUID courseId;

    private String title;

    private String description;

    private LocalDateTime dueDate;

    private boolean published;

    /** "REGULAR" or "PROGRAMMING". Defaults to REGULAR if omitted. */
    private String assignmentType;

    /** Required when assignmentType is "PROGRAMMING": "JAVA" | "PYTHON" | "C" | "CPP". */
    private String programmingLanguage;

    private Double maxScore;

    /** Percentage deducted per full day late, e.g. 10 = 10%/day. Omit or 0 for no penalty. */
    private Double latePenaltyPercent;

    /** Test cases for PROGRAMMING assignments. Ignored otherwise. */
    private List<TestCaseDto> testCases;
}
