package Final.year.project.SmartLearning.Assignments;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByAssignmentIdOrderBySequenceAsc(UUID assignmentId);

    void deleteByAssignmentId(UUID assignmentId);
}
