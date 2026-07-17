package Final.year.project.SmartLearning.Courses;

import Final.year.project.SmartLearning.dto.CreateCourseRequest;
import Final.year.project.SmartLearning.dto.CourseResponse;
import Final.year.project.SmartLearning.dto.UpdateCourseRequest;

import java.util.List;
import java.util.UUID;

public interface CourseService {

    CourseResponse createCourse(CreateCourseRequest request);

    CourseResponse getCourse(UUID id);

    List<CourseResponse> getAllCourses();

    void deleteCourse(UUID id);
    CourseResponse getCourseByCode(String courseCode);

    CourseResponse updateCourse(UUID id, UpdateCourseRequest request);

    List<CourseResponse> getCoursesByLecturer(UUID lecturerId);

    /** Courses taught by the currently authenticated lecturer. */
    List<CourseResponse> getMyCourses();
}