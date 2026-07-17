package Final.year.project.SmartLearning.dto;

import lombok.Data;

@Data
public class GenerateAssignmentRequest {

    /** What the lecturer typed, e.g. "We covered binary search trees today, give a programming assignment". */
    private String prompt;

    /** "REGULAR" or "PROGRAMMING". */
    private String assignmentType;

    /** Only used when assignmentType is "PROGRAMMING". */
    private String programmingLanguage;

    /** Roughly how many test cases to generate for programming assignments. Defaults to 4. */
    private Integer testCaseCount;
}
