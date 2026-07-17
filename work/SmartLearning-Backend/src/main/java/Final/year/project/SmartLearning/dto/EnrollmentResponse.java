package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EnrollmentResponse {

    private UUID id;

    private UUID studentId;

    private String studentName;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private LocalDateTime enrolledAt;

    private boolean active;
}