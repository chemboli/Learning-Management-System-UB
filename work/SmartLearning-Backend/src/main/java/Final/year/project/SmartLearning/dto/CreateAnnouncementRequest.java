package Final.year.project.SmartLearning.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CreateAnnouncementRequest {

    private String title;

    private String body;

    /** "LOW" | "NORMAL" | "HIGH" | "URGENT". Defaults to NORMAL if omitted. */
    private String priority;

    /** Null/omitted = sitewide announcement. */
    private UUID courseId;

    private boolean published = true;

    private LocalDateTime expiresAt;
}
