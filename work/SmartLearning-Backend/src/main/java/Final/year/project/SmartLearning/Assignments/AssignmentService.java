package Final.year.project.SmartLearning.Assignments;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Courses.CourseRepository;
import Final.year.project.SmartLearning.Submissions.Submission;
import Final.year.project.SmartLearning.Submissions.SubmissionRepository;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.AssignmentResponse;
import Final.year.project.SmartLearning.dto.CreateAssignmentRequest;
import Final.year.project.SmartLearning.dto.TestCaseDto;
import Final.year.project.SmartLearning.dto.UpdateAssignmentRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final TestCaseRepository testCaseRepository;

    public AssignmentResponse createAssignment(CreateAssignmentRequest request) {

        User user = currentUser();

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new RuntimeException("Course not found"));

        checkLecturerOwnsCourse(user, course);

        AssignmentType type = parseAssignmentType(request.getAssignmentType());
        ProgrammingLanguage language = type == AssignmentType.PROGRAMMING
                ? parseLanguage(request.getProgrammingLanguage())
                : null;

        if (type == AssignmentType.PROGRAMMING && language == null) {
            throw new RuntimeException("A programming language must be specified for programming assignments");
        }

        Assignment assignment = Assignment.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .dueDate(request.getDueDate())
                .published(request.isPublished())
                .assignmentType(type)
                .programmingLanguage(language)
                .maxScore(request.getMaxScore() != null ? request.getMaxScore() : 100.0)
                .latePenaltyPercent(request.getLatePenaltyPercent() != null ? request.getLatePenaltyPercent() : 0.0)
                .course(course)
                .lecturer(user)
                .createdAt(LocalDateTime.now())
                .build();

        assignment = assignmentRepository.save(assignment);

        if (type == AssignmentType.PROGRAMMING && request.getTestCases() != null) {
            saveTestCases(assignment, request.getTestCases());
        }

        return toResponse(assignment, user);
    }

    public List<AssignmentResponse> getCourseAssignments(UUID courseId) {

        User user = currentUser();
        boolean isStudent = user.getRole().name().equals("STUDENT");

        List<Assignment> assignments = isStudent
                ? assignmentRepository.findByCourseIdAndPublishedTrue(courseId)
                : assignmentRepository.findByCourseId(courseId);

        return assignments.stream()
                .sorted(Comparator.comparing(Assignment::getDueDate))
                .map(a -> toResponse(a, user))
                .toList();
    }

    public AssignmentResponse getAssignment(UUID id) {

        User user = currentUser();

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        return toResponse(assignment, user);
    }

    public AssignmentResponse updateAssignment(UUID id, UpdateAssignmentRequest request) {

        User user = currentUser();

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        checkLecturerOwnsCourse(user, assignment.getCourse());

        if (request.getTitle() != null) {
            assignment.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            assignment.setDescription(request.getDescription());
        }

        if (request.getDueDate() != null) {
            assignment.setDueDate(request.getDueDate());
        }

        if (request.getPublished() != null) {
            assignment.setPublished(request.getPublished());
        }

        if (request.getAssignmentType() != null) {
            assignment.setAssignmentType(parseAssignmentType(request.getAssignmentType()));
        }

        if (request.getProgrammingLanguage() != null) {
            assignment.setProgrammingLanguage(parseLanguage(request.getProgrammingLanguage()));
        }

        if (request.getMaxScore() != null) {
            assignment.setMaxScore(request.getMaxScore());
        }

        if (request.getLatePenaltyPercent() != null) {
            assignment.setLatePenaltyPercent(request.getLatePenaltyPercent());
        }

        assignment = assignmentRepository.save(assignment);

        if (assignment.getAssignmentType() == AssignmentType.PROGRAMMING && request.getTestCases() != null) {
            testCaseRepository.deleteByAssignmentId(assignment.getId());
            saveTestCases(assignment, request.getTestCases());
        }

        return toResponse(assignment, user);
    }
    @Transactional
    public void deleteAssignment(UUID id) {

        User user = currentUser();

        Assignment assignment = assignmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        checkLecturerOwnsCourse(user, assignment.getCourse());

        assignmentRepository.delete(assignment);
    }

    // ---- helpers -----------------------------------------------------------

    private void saveTestCases(Assignment assignment, List<TestCaseDto> dtos) {
        List<TestCase> entities = new ArrayList<>();
        int seq = 0;
        for (TestCaseDto dto : dtos) {
            entities.add(TestCase.builder()
                    .sequence(dto.getSequence() != 0 ? dto.getSequence() : seq)
                    .label(dto.getLabel())
                    .input(dto.getInput())
                    .expectedOutput(dto.getExpectedOutput())
                    .weight(dto.getWeight() > 0 ? dto.getWeight() : 1.0)
                    .hidden(dto.isHidden())
                    .assignment(assignment)
                    .build());
            seq++;
        }
        testCaseRepository.saveAll(entities);
    }

    private AssignmentType parseAssignmentType(String value) {
        if (value == null || value.isBlank()) return AssignmentType.REGULAR;
        try {
            return AssignmentType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid assignment type: " + value);
        }
    }

    private ProgrammingLanguage parseLanguage(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return ProgrammingLanguage.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid programming language: " + value);
        }
    }

    private User currentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void checkLecturerOwnsCourse(User user, Course course) {

        boolean isLecturer = user.getRole().name().equals("LECTURER");

        if (isLecturer && !course.getLecturer().getId().equals(user.getId())) {
            throw new RuntimeException("You are not assigned to this course");
        }
    }

    private AssignmentResponse toResponse(Assignment assignment, User currentUser) {

        boolean overdue = assignment.getDueDate() != null
                && LocalDateTime.now().isAfter(assignment.getDueDate());

        boolean isStudent = currentUser.getRole().name().equals("STUDENT");

        AssignmentResponse.AssignmentResponseBuilder builder = AssignmentResponse.builder()
                .id(assignment.getId())
                .title(assignment.getTitle())
                .description(assignment.getDescription())
                .dueDate(assignment.getDueDate())
                .published(assignment.isPublished())
                .overdue(overdue)
                .courseId(assignment.getCourse().getId())
                .courseCode(assignment.getCourse().getCourseCode())
                .courseTitle(assignment.getCourse().getTitle())
                .lecturerName(
                        assignment.getLecturer().getFirstName() + " " +
                        assignment.getLecturer().getLastName()
                )
                .createdAt(assignment.getCreatedAt())
                .assignmentType(assignment.getAssignmentType() != null
                        ? assignment.getAssignmentType().name() : "REGULAR")
                .programmingLanguage(assignment.getProgrammingLanguage() != null
                        ? assignment.getProgrammingLanguage().name() : null)
                .maxScore(assignment.getMaxScore())
                .latePenaltyPercent(assignment.getLatePenaltyPercent());

        if (assignment.getAssignmentType() == AssignmentType.PROGRAMMING) {
            List<TestCase> testCases = testCaseRepository
                    .findByAssignmentIdOrderBySequenceAsc(assignment.getId());

            // Students never see hidden test cases' input/expected output —
            // only that they exist, so they understand part of the grade is withheld until run.
            List<TestCaseDto> dtos = testCases.stream()
                    .map(tc -> TestCaseDto.builder()
                            .id(tc.getId())
                            .sequence(tc.getSequence())
                            .label(tc.getLabel())
                            .input(isStudent && tc.isHidden() ? null : tc.getInput())
                            .expectedOutput(isStudent && tc.isHidden() ? null : tc.getExpectedOutput())
                            .weight(tc.getWeight())
                            .hidden(tc.isHidden())
                            .build())
                    .toList();

            builder.testCases(dtos);
        }

        if (isStudent) {
            List<Submission> mine = submissionRepository
                    .findByAssignmentIdAndStudentIdOrderBySubmittedAtDesc(
                            assignment.getId(), currentUser.getId());

            String status = "NOT_SUBMITTED";

            if (!mine.isEmpty()) {
                status = mine.get(0).isReviewed() ? "REVIEWED" : "SUBMITTED";
            }

            builder.mySubmissionStatus(status);
        } else {
            List<Submission> all = submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignment.getId());

            builder.submissionCount(all.size());

            double avg = all.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore)
                    .average()
                    .orElse(0.0);

            builder.averageScore(Math.round(avg * 100.0) / 100.0);
        }

        return builder.build();
    }
}
