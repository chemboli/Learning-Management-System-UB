package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Notes.NoteType;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateNoteRequest {

    private UUID courseId;

    private String title;

    private String description;

    private NoteType noteType;

    private Integer weekNumber;

    private boolean published;
}