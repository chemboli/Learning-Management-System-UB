package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class UpdateCourseRequest {

    private String title;

    private String description;

    private Integer creditHours;

    private UUID lecturerId;

    private Boolean active;
}
