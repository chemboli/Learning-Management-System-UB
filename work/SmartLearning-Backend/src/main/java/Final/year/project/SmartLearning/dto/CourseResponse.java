package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class CourseResponse {

    private UUID id;

    private String courseCode;

    private String title;

    private String description;

    private Integer creditHours;

    private UUID lecturerId;

    private String lecturerName;

    private boolean active;

}