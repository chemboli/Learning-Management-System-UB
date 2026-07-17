package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Notes.NoteType;
import lombok.Data;

@Data
public class UpdateNoteRequest {

    private String title;

    private String description;

    private NoteType noteType;

    private Integer weekNumber;

    private Boolean published;
}
