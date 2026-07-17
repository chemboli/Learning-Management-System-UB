package Final.year.project.SmartLearning.Assignments;

import Final.year.project.SmartLearning.dto.AssignmentResponse;
import Final.year.project.SmartLearning.dto.CreateAssignmentRequest;
import Final.year.project.SmartLearning.dto.UpdateAssignmentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public AssignmentResponse createAssignment(
            @RequestBody CreateAssignmentRequest request) {

        return assignmentService.createAssignment(request);
    }

    @GetMapping("/course/{courseId}")
    public List<AssignmentResponse> getCourseAssignments(
            @PathVariable UUID courseId) {

        return assignmentService.getCourseAssignments(courseId);
    }

    @GetMapping("/{id}")
    public AssignmentResponse getAssignment(
            @PathVariable UUID id) {

        return assignmentService.getAssignment(id);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public AssignmentResponse updateAssignment(
            @PathVariable UUID id,
            @RequestBody UpdateAssignmentRequest request) {

        return assignmentService.updateAssignment(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LECTURER','ADMIN','MASTER')")
    public void deleteAssignment(
            @PathVariable UUID id) {

        assignmentService.deleteAssignment(id);
    }
}
