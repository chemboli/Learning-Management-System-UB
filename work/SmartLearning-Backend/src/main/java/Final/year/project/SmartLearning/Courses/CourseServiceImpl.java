package Final.year.project.SmartLearning.Courses;

import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.CourseResponse;
import Final.year.project.SmartLearning.dto.CreateCourseRequest;
import Final.year.project.SmartLearning.dto.UpdateCourseRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

    @Override
    public CourseResponse createCourse(CreateCourseRequest request) {

        if (courseRepository.existsByCourseCode(request.getCourseCode())) {
            throw new RuntimeException("Course code already exists");
        }

        User lecturer = userRepository.findById(request.getLecturerId())
                .orElseThrow(() -> new RuntimeException("Lecturer not found"));

        Course course = Course.builder()
                .courseCode(request.getCourseCode())
                .title(request.getTitle())
                .description(request.getDescription())
                .creditHours(request.getCreditHours())
                .lecturer(lecturer)
                .active(true)
                .build();

        course = courseRepository.save(course);

        return mapToResponse(course);
    }

    @Override
    public CourseResponse getCourse(UUID id) {

        return courseRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("Course not found"));
    }

    @Override
    public List<CourseResponse> getAllCourses() {

        return courseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteCourse(UUID id) {

        if (!courseRepository.existsById(id)) {
            throw new RuntimeException("Course not found");
        }

        courseRepository.deleteById(id);
    }

    private CourseResponse mapToResponse(Course course) {

        return CourseResponse.builder()
                .id(course.getId())
                .courseCode(course.getCourseCode())
                .title(course.getTitle())
                .description(course.getDescription())
                .creditHours(course.getCreditHours())
                .lecturerId(course.getLecturer().getId())
                .lecturerName(
                        course.getLecturer().getFirstName() + " " +
                        course.getLecturer().getLastName()
                )
                .active(course.isActive())
                .build();
    }
    @Override
    public CourseResponse getCourseByCode(String courseCode) {

        return courseRepository.findByCourseCode(courseCode)
                .map(this::mapToResponse)
                .orElseThrow(() ->
                        new RuntimeException("Course not found"));
    }

    @Override
    public CourseResponse updateCourse(UUID id, UpdateCourseRequest request) {

        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        if (request.getTitle() != null) {
            course.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            course.setDescription(request.getDescription());
        }

        if (request.getCreditHours() != null) {
            course.setCreditHours(request.getCreditHours());
        }

        if (request.getActive() != null) {
            course.setActive(request.getActive());
        }

        if (request.getLecturerId() != null) {
            User lecturer = userRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new RuntimeException("Lecturer not found"));
            course.setLecturer(lecturer);
        }

        course = courseRepository.save(course);

        return mapToResponse(course);
    }

    @Override
    public List<CourseResponse> getCoursesByLecturer(UUID lecturerId) {

        return courseRepository.findByLecturerId(lecturerId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<CourseResponse> getMyCourses() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User lecturer = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return courseRepository.findByLecturerId(lecturer.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }
}