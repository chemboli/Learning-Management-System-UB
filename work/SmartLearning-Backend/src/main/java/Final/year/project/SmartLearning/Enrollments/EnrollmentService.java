package Final.year.project.SmartLearning.Enrollments;

import Final.year.project.SmartLearning.dto.EnrollRequest;
import Final.year.project.SmartLearning.dto.EnrollmentResponse;

import java.util.List;
import java.util.UUID;

public interface EnrollmentService {

    EnrollmentResponse enrollStudent(
            EnrollRequest request);

    void unenrollStudent(UUID courseId);

    boolean isEnrolled(UUID courseId);

    List<EnrollmentResponse> getMyCourses();

    List<EnrollmentResponse> getEnrollmentsByStudent(
            UUID studentId);

    List<EnrollmentResponse> getEnrollmentsByCourse(
            UUID courseId);

    void removeEnrollment(UUID enrollmentId);

    /**
     * CSV export of the course roster, including each student's score so far
     * (sum of graded submissions in this course). Lecturer must own the
     * course; admin/master may export any course.
     */
    String exportRosterCsv(UUID courseId);
}