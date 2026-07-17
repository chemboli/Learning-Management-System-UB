package Final.year.project.SmartLearning.Courses;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByCourseCode(String courseCode);

    List<Course> findByLecturerId(UUID lecturerId);

    boolean existsByCourseCode(String courseCode);
}