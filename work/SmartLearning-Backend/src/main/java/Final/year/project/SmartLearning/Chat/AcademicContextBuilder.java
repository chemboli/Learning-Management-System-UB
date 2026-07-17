package Final.year.project.SmartLearning.Chat;

import Final.year.project.SmartLearning.Assignments.Assignment;
import Final.year.project.SmartLearning.Assignments.AssignmentRepository;
import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Courses.CourseRepository;
import Final.year.project.SmartLearning.Enrollments.Enrollment;
import Final.year.project.SmartLearning.Enrollments.EnrollmentRepository;
import Final.year.project.SmartLearning.Submissions.Submission;
import Final.year.project.SmartLearning.Submissions.SubmissionRepository;
import Final.year.project.SmartLearning.Users.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Pulls a user's real course/assignment/submission data and turns it into a
 * compact text summary handed to the AI as grounding context — so chat
 * recommendations are based on this person's actual standing, not generic
 * advice. Strictly scoped per role:
 *   - Students only ever see their own enrolled courses and submissions.
 *   - Lecturers only ever see courses they personally teach.
 * This scoping happens here, server-side, before anything reaches the AI —
 * the model never sees another student's data or a course a lecturer
 * doesn't own.
 */
@Component
@RequiredArgsConstructor
public class AcademicContextBuilder {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    public String buildContext(User user) {
        String role = user.getRole().name();

        return switch (role) {
            case "STUDENT" -> buildStudentContext(user);
            case "LECTURER" -> buildLecturerContext(user);
            default -> "This user is a " + role + ". No course-specific data applies to this role.";
        };
    }

    // ---- Student context ----------------------------------------------

    private String buildStudentContext(User student) {

        List<Course> courses = enrollmentRepository.findByStudentId(student.getId())
                .stream()
                .map(Enrollment::getCourse)
                .toList();

        if (courses.isEmpty()) {
            return "This student is not currently enrolled in any courses.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Student: ").append(student.getFirstName()).append(" ").append(student.getLastName()).append("\n");
        sb.append("Enrolled in ").append(courses.size()).append(" course(s):\n\n");

        for (Course course : courses) {
            sb.append("Course: ").append(course.getCourseCode()).append(" — ").append(course.getTitle()).append("\n");

            List<Assignment> assignments = assignmentRepository.findByCourseIdAndPublishedTrue(course.getId());

            if (assignments.isEmpty()) {
                sb.append("  No assignments posted yet.\n\n");
                continue;
            }

            for (Assignment assignment : assignments) {
                List<Submission> mine = submissionRepository
                        .findByAssignmentIdAndStudentId(assignment.getId(), student.getId());

                sb.append("  - Assignment: \"").append(assignment.getTitle()).append("\"")
                        .append(" [").append(assignment.getAssignmentType()).append("]")
                        .append(", due ").append(formatDate(assignment.getDueDate()))
                        .append(", max score ").append(assignment.getMaxScore())
                        .append("\n");

                if (mine.isEmpty()) {
                    boolean overdue = assignment.getDueDate() != null
                            && LocalDateTime.now().isAfter(assignment.getDueDate());
                    sb.append("    Status: NOT SUBMITTED")
                            .append(overdue ? " (past due)" : " (not yet due)")
                            .append("\n");
                } else {
                    Submission s = mine.get(0);
                    sb.append("    Status: submitted ").append(formatDate(s.getSubmittedAt()));
                    if (s.getScore() != null) {
                        sb.append(", score ").append(s.getScore()).append("/").append(assignment.getMaxScore());
                    } else {
                        sb.append(", not yet graded");
                    }
                    if (s.getFeedback() != null && !s.getFeedback().isBlank()) {
                        sb.append(", lecturer feedback: \"").append(truncate(s.getFeedback(), 200)).append("\"");
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ---- Lecturer context -----------------------------------------------

    private String buildLecturerContext(User lecturer) {

        List<Course> courses = courseRepository.findByLecturerId(lecturer.getId());

        if (courses.isEmpty()) {
            return "This lecturer is not currently assigned to teach any courses.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Lecturer: ").append(lecturer.getFirstName()).append(" ").append(lecturer.getLastName()).append("\n");
        sb.append("Teaching ").append(courses.size()).append(" course(s):\n\n");

        for (Course course : courses) {
            sb.append("Course: ").append(course.getCourseCode()).append(" — ").append(course.getTitle()).append("\n");

            List<Assignment> assignments = assignmentRepository.findByCourseId(course.getId());

            if (assignments.isEmpty()) {
                sb.append("  No assignments created yet.\n\n");
                continue;
            }

            for (Assignment assignment : assignments) {
                List<Submission> submissions = submissionRepository
                        .findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());

                long gradedCount = submissions.stream().filter(s -> s.getScore() != null).count();
                long ungradedCount = submissions.size() - gradedCount;

                sb.append("  - Assignment: \"").append(assignment.getTitle()).append("\"")
                        .append(" [").append(assignment.getAssignmentType()).append("]")
                        .append(assignment.isPublished() ? "" : " (DRAFT, not published)")
                        .append(", due ").append(formatDate(assignment.getDueDate()))
                        .append("\n")
                        .append("    Submissions: ").append(submissions.size())
                        .append(" total, ").append(gradedCount).append(" graded, ")
                        .append(ungradedCount).append(" awaiting grading\n");

                if (!submissions.isEmpty() && gradedCount > 0) {
                    double avg = submissions.stream()
                            .filter(s -> s.getScore() != null)
                            .mapToDouble(Submission::getScore)
                            .average()
                            .orElse(0);
                    sb.append("    Average score so far: ")
                            .append(String.format("%.1f", avg)).append("/").append(assignment.getMaxScore())
                            .append("\n");
                }
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    // ---- helpers -----------------------------------------------------------

    private String formatDate(LocalDateTime dt) {
        return dt != null ? dt.format(DATE_FMT) : "no due date set";
    }

    private String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
