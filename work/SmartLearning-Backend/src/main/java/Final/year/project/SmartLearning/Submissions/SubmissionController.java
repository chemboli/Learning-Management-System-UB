package Final.year.project.SmartLearning.Submissions;

import Final.year.project.SmartLearning.dto.BatchRunResponse;
import Final.year.project.SmartLearning.dto.CreateSubmissionRequest;
import Final.year.project.SmartLearning.dto.GradeSubmissionRequest;
import Final.year.project.SmartLearning.dto.SubmissionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping(
            value = "/submit",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('STUDENT')")
    public SubmissionResponse submit(
            @RequestPart("file") MultipartFile file,

            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(
                                    implementation = CreateSubmissionRequest.class
                            )
                    )
            )
            @RequestPart("request") CreateSubmissionRequest request
    ) {
        return submissionService.submit(file, request);
    }

    @GetMapping("/assignment/{assignmentId}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public List<SubmissionResponse> getAssignmentSubmissions(
            @PathVariable UUID assignmentId) {

        return submissionService.getAssignmentSubmissions(assignmentId);
    }

    @GetMapping("/assignment/{assignmentId}/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public List<SubmissionResponse> getMySubmissions(
            @PathVariable UUID assignmentId) {

        return submissionService.getMySubmissions(assignmentId);
    }

    /**
     * Withdraws the current student's own submission for this assignment so
     * they can submit again. Blocked once the submission has been manually
     * graded by a lecturer.
     */
    @DeleteMapping("/assignment/{assignmentId}/mine")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Void> unsubmit(
            @PathVariable UUID assignmentId) {

        submissionService.unsubmit(assignmentId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public SubmissionResponse markReviewed(
            @PathVariable UUID id) {

        return submissionService.markReviewed(id);
    }

    /** Lecturer manually enters/edits a score and feedback for a submission. */
    @PutMapping("/{id}/grade")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public SubmissionResponse gradeSubmission(
            @PathVariable UUID id,
            @RequestBody GradeSubmissionRequest request) {

        return submissionService.gradeSubmission(id, request);
    }

    /**
     * Runs the submitted source code against the assignment's test cases and shows
     * the lecturer the actual output. Lecturer/admin only — students cannot trigger
     * this themselves; results from the automatic run-on-submit are still visible
     * to students as read-only feedback (see SubmissionResponse.lastRunResult).
     */
    @PostMapping("/{id}/run")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public SubmissionResponse runSubmission(
            @PathVariable UUID id) {

        return submissionService.runSubmission(id);
    }

    /**
     * Runs every submission for a programming assignment in one batch.
     * Lecturer/admin only.
     */
    @PostMapping("/assignment/{assignmentId}/run-all")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public BatchRunResponse runAllForAssignment(
            @PathVariable UUID assignmentId) {

        return submissionService.runAllForAssignment(assignmentId);
    }

    /**
     * Runs an AI-likelihood analysis on this submission's text content.
     * Returns a 0-100 estimate + explanation — a triage signal for the lecturer,
     * never an automatic verdict. Lecturer/admin only.
     */
    @PostMapping("/{id}/analyze-ai")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public SubmissionResponse analyzeForAi(
            @PathVariable UUID id) {

        return submissionService.analyzeForAi(id);
    }

    /**
     * Downloads a CSV gradebook for this assignment — one row per enrolled
     * student, including those who haven't submitted yet. Lecturer/admin only.
     */
    @GetMapping("/assignment/{assignmentId}/export")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public ResponseEntity<byte[]> exportScores(@PathVariable UUID assignmentId) {

        String csv = submissionService.exportScoresCsv(assignmentId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"scores-" + assignmentId + ".csv\"")
                .body(bytes);
    }
}
