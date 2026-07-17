package Final.year.project.SmartLearning.Submissions;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SubmissionRepository extends JpaRepository<Submission, UUID> {

    List<Submission> findByAssignmentIdOrderBySubmittedAtDesc(UUID assignmentId);

    List<Submission> findByAssignmentIdAndStudentIdOrderBySubmittedAtDesc(UUID assignmentId, UUID studentId);

    /**
     * A student should have at most one submission per assignment going
     * forward (enforced in SubmissionService). This intentionally returns a
     * List rather than a single Optional: if any data from before this rule
     * existed (a student who submitted more than once under the old
     * behavior) still has multiple rows, a single-result query would throw
     * at runtime instead of just picking the most recent one.
     */
    List<Submission> findByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    List<Submission> findByStudentIdOrderBySubmittedAtDesc(UUID studentId);

    long countByAssignmentId(UUID assignmentId);

    long countByAssignmentIdAndStudentId(UUID assignmentId, UUID studentId);

    /** Used to total a student's scores across every assignment in one course (roster export). */
    List<Submission> findByStudentIdAndAssignmentIdIn(UUID studentId, List<UUID> assignmentIds);
}
