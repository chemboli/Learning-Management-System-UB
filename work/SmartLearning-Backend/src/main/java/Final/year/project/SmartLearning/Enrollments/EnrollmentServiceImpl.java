package Final.year.project.SmartLearning.Enrollments;

import Final.year.project.SmartLearning.Assignments.Assignment;
import Final.year.project.SmartLearning.Assignments.AssignmentRepository;
import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Courses.CourseRepository;
import Final.year.project.SmartLearning.Submissions.Submission;
import Final.year.project.SmartLearning.Submissions.SubmissionRepository;
import Final.year.project.SmartLearning.Users.Role;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.EnrollRequest;
import Final.year.project.SmartLearning.dto.EnrollmentResponse;
import Final.year.project.SmartLearning.shared.CsvWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EnrollmentServiceImpl
        implements EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    @Override
    public EnrollmentResponse enrollStudent(
            EnrollRequest request) {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        String email = authentication.getName();

        User student = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new RuntimeException(
                    "Only students can enroll");
        }

        Course course = courseRepository
                .findById(request.getCourseId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Course not found"));

        if (enrollmentRepository
                .findByStudentIdAndCourseId(
                        student.getId(),
                        course.getId())
                .isPresent()) {

            throw new RuntimeException(
                    "Already enrolled");
        }

        Enrollment enrollment =
                Enrollment.builder()
                        .student(student)
                        .course(course)
                        .enrolledAt(
                                LocalDateTime.now())
                        .active(true)
                        .build();

        enrollment =
                enrollmentRepository
                        .save(enrollment);

        return mapToResponse(enrollment);
    }

    @Override
    public List<EnrollmentResponse> getMyCourses() {

        String email =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getName();

        User student = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        return enrollmentRepository
                .findByStudentId(student.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<EnrollmentResponse>
    getEnrollmentsByStudent(UUID studentId) {

        return enrollmentRepository
                .findByStudentId(studentId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<EnrollmentResponse>
    getEnrollmentsByCourse(UUID courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User caller = currentUser();
        boolean isLecturer = caller.getRole() == Role.LECTURER;

        if (isLecturer && !course.getLecturer().getId().equals(caller.getId())) {
            throw new RuntimeException("You are not assigned to this course");
        }

        return enrollmentRepository
                .findByCourseId(courseId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void removeEnrollment(UUID enrollmentId) {

        enrollmentRepository
                .deleteById(enrollmentId);
    }

    @Override
    public void unenrollStudent(UUID courseId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User student = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(
                        student.getId(),
                        courseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Enrollment not found"));

        enrollmentRepository.delete(enrollment);
    }

    @Override
    public boolean isEnrolled(UUID courseId) {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        User student = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Student not found"));

        return enrollmentRepository
                .findByStudentIdAndCourseId(
                        student.getId(),
                        courseId)
                .isPresent();
    }

    private EnrollmentResponse mapToResponse(
            Enrollment enrollment) {

        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(
                        enrollment.getStudent().getId())
                .studentName(
                        enrollment.getStudent()
                                .getFirstName()
                                + " "
                                + enrollment.getStudent()
                                .getLastName())
                .courseId(
                        enrollment.getCourse().getId())
                .courseCode(
                        enrollment.getCourse()
                                .getCourseCode())
                .courseTitle(
                        enrollment.getCourse()
                                .getTitle())
                .enrolledAt(
                        enrollment.getEnrolledAt())
                .active(
                        enrollment.isActive())
                .build();
    }

    @Override
    public String exportRosterCsv(UUID courseId) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Course not found"));

        User caller = currentUser();
        boolean isLecturer = caller.getRole() == Role.LECTURER;

        if (isLecturer && !course.getLecturer().getId().equals(caller.getId())) {
            throw new RuntimeException("You are not assigned to this course");
        }

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);
        List<Assignment> assignments = assignmentRepository.findByCourseId(courseId);
        List<UUID> assignmentIds = assignments.stream().map(Assignment::getId).toList();

        List<String> header = List.of("Student ID", "Student Name", "Student Email", "Total Score", "Enrolled At");
        List<List<String>> rows = new java.util.ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();

            double totalScore = 0;
            if (!assignmentIds.isEmpty()) {
                List<Submission> submissions = submissionRepository
                        .findByStudentIdAndAssignmentIdIn(student.getId(), assignmentIds);
                totalScore = submissions.stream()
                        .filter(s -> s.getScore() != null)
                        .mapToDouble(Submission::getScore)
                        .sum();
            }

            rows.add(List.of(
                    student.getId().toString(),
                    student.getFirstName() + " " + student.getLastName(),
                    student.getEmail(),
                    String.valueOf(totalScore),
                    enrollment.getEnrolledAt() != null ? enrollment.getEnrolledAt().toString() : ""
            ));
        }

        return CsvWriter.write(header, rows);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}