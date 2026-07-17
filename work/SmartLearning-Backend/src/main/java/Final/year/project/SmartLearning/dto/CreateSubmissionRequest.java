package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateSubmissionRequest {

    private UUID assignmentId;

    private String comment;
}
