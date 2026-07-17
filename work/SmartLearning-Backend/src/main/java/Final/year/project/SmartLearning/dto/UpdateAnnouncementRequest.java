package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateAnnouncementRequest {

    private String title;

    private String body;

    private String priority;

    private Boolean published;

    private LocalDateTime expiresAt;
}
