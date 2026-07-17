package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UpdateAssignmentRequest {

    private String title;

    private String description;

    private LocalDateTime dueDate;

    private Boolean published;

    private String assignmentType;

    private String programmingLanguage;

    private Double maxScore;

    private Double latePenaltyPercent;

    private List<TestCaseDto> testCases;
}
