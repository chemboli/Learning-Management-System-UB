package Final.year.project.SmartLearning.Enrollments;

import Final.year.project.SmartLearning.dto.EnrollRequest;
import Final.year.project.SmartLearning.dto.EnrollmentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping("/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public EnrollmentResponse enroll(
            @RequestBody EnrollRequest request) {

        return enrollmentService
                .enrollStudent(request);
    }

    @DeleteMapping("/unenroll/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public void unenroll(
            @PathVariable UUID courseId) {

        enrollmentService
                .unenrollStudent(courseId);
    }

    @GetMapping("/check/{courseId}")
    @PreAuthorize("hasRole('STUDENT')")
    public boolean checkEnrollment(
            @PathVariable UUID courseId) {

        return enrollmentService
                .isEnrolled(courseId);
    }

    @GetMapping("/my-courses")
    @PreAuthorize("hasRole('STUDENT')")
    public List<EnrollmentResponse> getMyCourses() {

        return enrollmentService
                .getMyCourses();
    }

    @GetMapping("/student/{studentId}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public List<EnrollmentResponse>
    getEnrollmentsByStudent(
            @PathVariable UUID studentId) {

        return enrollmentService
                .getEnrollmentsByStudent(
                        studentId);
    }

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN','LECTURER')")
    public List<EnrollmentResponse>
    getEnrollmentsByCourse(
            @PathVariable UUID courseId) {

        return enrollmentService
                .getEnrollmentsByCourse(
                        courseId);
    }

    /**
     * Downloads a CSV roster for a course — every enrolled student with
     * their total score across the course's assignments. Lecturer must own
     * the course; admin/master may export any course's roster.
     */
    @GetMapping("/course/{courseId}/export")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN','LECTURER')")
    public ResponseEntity<byte[]> exportRoster(
            @PathVariable UUID courseId) {

        String csv = enrollmentService.exportRosterCsv(courseId);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"roster-" + courseId + ".csv\"")
                .body(bytes);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public void removeEnrollment(
            @PathVariable UUID id) {

        enrollmentService
                .removeEnrollment(id);
    }
}