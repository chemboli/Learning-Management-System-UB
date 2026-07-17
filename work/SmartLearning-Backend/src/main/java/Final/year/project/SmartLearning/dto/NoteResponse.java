package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Notes.NoteType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class NoteResponse {

    private UUID id;

    private String title;

    private String description;

    private NoteType noteType;

    private Integer weekNumber;

    private String downloadUrl;

    private String fileName;

    private String contentType;

    private long fileSize;

    private String courseTitle;

    private String lecturerName;

    private LocalDateTime uploadedAt;
}