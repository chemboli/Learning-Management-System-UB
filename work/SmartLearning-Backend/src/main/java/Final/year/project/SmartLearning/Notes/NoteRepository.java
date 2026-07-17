package Final.year.project.SmartLearning.Notes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {

    List<Note> findByCourseId(UUID courseId);
    List<Note> findByCourseCourseCode(String courseCode);
    List<Note> findByCourseIdAndPublishedTrue(UUID courseId);
}