package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AnnouncementResponse {

    private UUID id;

    private String title;

    private String body;

    private String priority;

    private UUID courseId;

    private String courseCode;

    private String courseTitle;

    private boolean sitewide;

    private String authorName;

    private boolean published;

    private boolean expired;

    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
