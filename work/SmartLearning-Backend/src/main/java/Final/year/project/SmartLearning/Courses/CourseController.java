package Final.year.project.SmartLearning.Courses;

import Final.year.project.SmartLearning.dto.CourseResponse;
import Final.year.project.SmartLearning.dto.CreateCourseRequest;
import Final.year.project.SmartLearning.dto.UpdateCourseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @PostMapping
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public CourseResponse createCourse(
            @RequestBody CreateCourseRequest request) {

        return courseService.createCourse(request);
    }

    @GetMapping("/{id}")
    public CourseResponse getCourse(
            @PathVariable UUID id) {

        return courseService.getCourse(id);
    }

    @GetMapping
    public List<CourseResponse> getAllCourses() {

        return courseService.getAllCourses();
    }

    /** Courses taught by the currently authenticated lecturer. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('LECTURER')")
    public List<CourseResponse> getMyCourses() {

        return courseService.getMyCourses();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public void deleteCourse(
            @PathVariable UUID id) {

        courseService.deleteCourse(id);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public CourseResponse updateCourse(
            @PathVariable UUID id,
            @RequestBody UpdateCourseRequest request) {

        return courseService.updateCourse(id, request);
    }

    @GetMapping("/lecturer/{lecturerId}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN','LECTURER')")
    public List<CourseResponse> getCoursesByLecturer(
            @PathVariable UUID lecturerId) {

        return courseService.getCoursesByLecturer(lecturerId);
    }

    @GetMapping("/code/{courseCode}")
    public CourseResponse getCourseByCode(
            @PathVariable String courseCode) {

        return courseService.getCourseByCode(courseCode);
    }
}