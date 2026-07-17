package Final.year.project.SmartLearning.Assignments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findByCourseId(UUID courseId);

    List<Assignment> findByCourseIdAndPublishedTrue(UUID courseId);

    List<Assignment> findByLecturerId(UUID lecturerId);
}
