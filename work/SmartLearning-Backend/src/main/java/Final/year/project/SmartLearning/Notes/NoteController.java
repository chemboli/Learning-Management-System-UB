package Final.year.project.SmartLearning.Notes;

import Final.year.project.SmartLearning.dto.CreateNoteRequest;
import Final.year.project.SmartLearning.dto.NoteResponse;
import Final.year.project.SmartLearning.dto.UpdateNoteRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public NoteResponse upload(
            @RequestPart("file") MultipartFile file,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = CreateNoteRequest.class
                            )
                    )
            )
            @RequestPart("request") CreateNoteRequest request
    ) {
        return noteService.upload(file, request);
    }
    @GetMapping("/course/{courseId}")
    public List<NoteResponse> getCourseNotes(
            @PathVariable UUID courseId) {

        return noteService.getCourseNotes(courseId);
    }

    @GetMapping("/course/code/{courseCode}")
    public List<NoteResponse> getByCourseCode(
            @PathVariable String courseCode) {

        return noteService.getByCourseCode(courseCode);
    }

    @GetMapping("/{id}")
    public NoteResponse getNote(
            @PathVariable UUID id) {

        return noteService.getNote(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public NoteResponse updateNote(
            @PathVariable UUID id,
            @RequestBody UpdateNoteRequest request) {

        return noteService.updateNote(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public void deleteNote(
            @PathVariable UUID id) {

        noteService.deleteNote(id);
    }
}