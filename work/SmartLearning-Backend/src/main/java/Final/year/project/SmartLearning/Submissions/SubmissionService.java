package Final.year.project.SmartLearning.Submissions;

import Final.year.project.SmartLearning.Assignments.Assignment;
import Final.year.project.SmartLearning.Assignments.AssignmentRepository;
import Final.year.project.SmartLearning.Assignments.AssignmentType;
import Final.year.project.SmartLearning.Assignments.TestCase;
import Final.year.project.SmartLearning.Assignments.TestCaseRepository;
import Final.year.project.SmartLearning.Ai.AiService;
import Final.year.project.SmartLearning.Enrollments.Enrollment;
import Final.year.project.SmartLearning.Enrollments.EnrollmentRepository;
import Final.year.project.SmartLearning.Execution.CodeExecutionService;
import Final.year.project.SmartLearning.Execution.ExecutionRunResult;
import Final.year.project.SmartLearning.Execution.TestCaseResult;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.AiAnalysisResponse;
import Final.year.project.SmartLearning.dto.BatchRunResponse;
import Final.year.project.SmartLearning.dto.CreateSubmissionRequest;
import Final.year.project.SmartLearning.dto.GradeSubmissionRequest;
import Final.year.project.SmartLearning.dto.SubmissionResponse;
import Final.year.project.SmartLearning.shared.CsvWriter;
import Final.year.project.SmartLearning.storage.MinioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final TestCaseRepository testCaseRepository;
    private final MinioService minioService;
    private final CodeExecutionService codeExecutionService;
    private final AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Qualifier("publicMinioClient")
    private final MinioClient publicMinioClient;

    /** File extensions that we treat as plain-text source code for the auto-grader. */
    private static final Set<String> CODE_EXTENSIONS = Set.of(
            "java", "py", "c", "cpp", "cc", "cxx", "h", "hpp", "txt"
    );

    public SubmissionResponse submit(MultipartFile file, CreateSubmissionRequest request) {

        User student = currentUser();

        Assignment assignment = assignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        boolean isStudent = student.getRole().name().equals("STUDENT");

        if (!isStudent) {
            throw new RuntimeException("Only students can submit assignments");
        }

        boolean enrolled = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), assignment.getCourse().getId())
                .isPresent();

        if (!enrolled) {
            throw new RuntimeException("You must be enrolled in this course to submit");
        }

        boolean alreadySubmitted = !submissionRepository
                .findByAssignmentIdAndStudentId(assignment.getId(), student.getId())
                .isEmpty();

        if (alreadySubmitted) {
            throw new RuntimeException(
                    "You've already submitted this assignment. Unsubmit first if you want to replace it."
            );
        }

        String objectName = minioService.uploadFile(
                file,
                "submissions/" + assignment.getCourse().getCourseCode() + "/" + assignment.getId()
        );

        String sourceCode = extractSourceCodeIfPossible(file);

        Submission submission = Submission.builder()
                .fileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .objectName(objectName)
                .comment(request.getComment())
                .reviewed(false)
                .submittedAt(LocalDateTime.now())
                .sourceCode(sourceCode)
                .assignment(assignment)
                .student(student)
                .build();

        submission = submissionRepository.save(submission);

        return toResponse(submission, student);
    }

    /**
     * Lets a student withdraw their own submission so they can submit again.
     * Blocked once a lecturer has manually graded the submission — a student
     * shouldn't be able to dodge a grade they don't like by unsubmitting after
     * the fact. Automated (un-reviewed) grades from a programming-assignment
     * run don't count as a block, since those are routine and not a final
     * judgement call.
     */
    public void unsubmit(UUID assignmentId) {

        User student = currentUser();

        List<Submission> existing = submissionRepository
                .findByAssignmentIdAndStudentId(assignmentId, student.getId());

        if (existing.isEmpty()) {
            throw new RuntimeException("You don't have a submission for this assignment");
        }

        // Normally there's exactly one. If older data somehow left more than
        // one behind, treat the most recent as "the" submission for the grading
        // check below, but clean up every row so the student isn't stuck.
        Submission mostRecent = existing.stream()
                .max(Comparator.comparing(Submission::getSubmittedAt))
                .orElseThrow();

        boolean manuallyGraded = mostRecent.getScore() != null && !mostRecent.isAutoGraded();

        if (manuallyGraded) {
            throw new RuntimeException(
                    "This submission has already been graded by your lecturer and can no longer be withdrawn. " +
                    "Contact your lecturer if you need to resubmit."
            );
        }

        for (Submission submission : existing) {
            try {
                minioService.deleteFile(submission.getObjectName());
            } catch (Exception e) {
                // Don't block the unsubmit if storage cleanup fails — an orphaned
                // file in MinIO is a minor cost, but a stuck "can't resubmit" state
                // for the student is a much worse outcome.
            }
        }

        submissionRepository.deleteAll(existing);
    }

    /** All submissions for an assignment, most recent first — lecturer/admin view. */
    public List<SubmissionResponse> getAssignmentSubmissions(UUID assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        User viewer = currentUser();
        checkLecturerOwnsCourse(viewer, assignment);

        return submissionRepository
                .findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)
                .stream()
                .map(s -> toResponse(s, viewer))
                .toList();
    }

    /** The current student's own submission history for one assignment, most recent first. */
    public List<SubmissionResponse> getMySubmissions(UUID assignmentId) {

        User student = currentUser();

        return submissionRepository
                .findByAssignmentIdAndStudentIdOrderBySubmittedAtDesc(assignmentId, student.getId())
                .stream()
                .map(s -> toResponse(s, student))
                .toList();
    }

    /**
     * Builds a CSV gradebook for one assignment: every student enrolled in the
     * course gets a row, even if they never submitted (shown as "Not submitted"
     * rather than silently omitted, so the lecturer sees the full picture).
     * Lecturer/admin only.
     */
    public String exportScoresCsv(UUID assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        checkLecturerOwnsCourse(currentUser(), assignment);

        List<Enrollment> enrollments = enrollmentRepository.findByCourseId(assignment.getCourse().getId());

        // Most recent submission per student, since a student may have submitted more than once.
        Map<UUID, Submission> latestByStudent = new HashMap<>();
        for (Submission s : submissionRepository.findByAssignmentIdOrderBySubmittedAtDesc(assignmentId)) {
            latestByStudent.putIfAbsent(s.getStudent().getId(), s);
        }

        List<String> header = List.of(
                "Student Name", "Email", "Matricule", "Status", "Score", "Max Score",
                "Percentage", "Late", "Auto-graded", "Submitted At", "Graded By", "Feedback"
        );

        List<List<String>> rows = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            User student = enrollment.getStudent();
            Submission submission = latestByStudent.get(student.getId());

            if (submission == null) {
                rows.add(List.of(
                        student.getFirstName() + " " + student.getLastName(),
                        student.getEmail(),
                        student.getMatricule() != null ? student.getMatricule() : "",
                        "Not submitted",
                        "", "", "", "", "", "", "", ""
                ));
                continue;
            }

            boolean late = assignment.getDueDate() != null
                    && submission.getSubmittedAt().isAfter(assignment.getDueDate());

            String percentage = submission.getScore() != null
                    ? String.format("%.1f%%", (submission.getScore() / assignment.getMaxScore()) * 100)
                    : "";

            rows.add(List.of(
                    student.getFirstName() + " " + student.getLastName(),
                    student.getEmail(),
                    student.getMatricule() != null ? student.getMatricule() : "",
                    submission.isReviewed() ? "Reviewed" : "Submitted",
                    submission.getScore() != null ? String.valueOf(submission.getScore()) : "",
                    String.valueOf(assignment.getMaxScore()),
                    percentage,
                    late ? "Yes" : "No",
                    submission.isAutoGraded() ? "Yes" : "No",
                    submission.getSubmittedAt().toString(),
                    submission.getGradedBy() != null
                            ? submission.getGradedBy().getFirstName() + " " + submission.getGradedBy().getLastName()
                            : (submission.isAutoGraded() ? "Automated grader" : ""),
                    submission.getFeedback() != null ? submission.getFeedback() : ""
            ));
        }

        return CsvWriter.write(header, rows);
    }

    public SubmissionResponse markReviewed(UUID submissionId) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        User viewer = currentUser();
        checkLecturerOwnsCourse(viewer, submission.getAssignment());

        submission.setReviewed(true);
        submission.setReviewedAt(LocalDateTime.now());

        submission = submissionRepository.save(submission);

        return toResponse(submission, viewer);
    }

    /** Lecturer manually enters/overrides a score and feedback for a submission. */
    public SubmissionResponse gradeSubmission(UUID submissionId, GradeSubmissionRequest request) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        User grader = currentUser();
        checkLecturerOwnsCourse(grader, submission.getAssignment());

        double max = submission.getAssignment().getMaxScore();

        if (request.getScore() != null) {
            if (request.getScore() < 0 || request.getScore() > max) {
                throw new RuntimeException("Score must be between 0 and " + max);
            }
            submission.setScore(request.getScore());
        }

        if (request.getFeedback() != null) {
            submission.setFeedback(request.getFeedback());
        }

        submission.setAutoGraded(false);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(grader);
        submission.setReviewed(true);
        if (submission.getReviewedAt() == null) {
            submission.setReviewedAt(LocalDateTime.now());
        }

        submission = submissionRepository.save(submission);

        return toResponse(submission, grader);
    }

    /**
     * Runs the submission's source code against the assignment's test cases and
     * stores the result. Only valid for PROGRAMMING assignments. Lecturer/admin
     * only — students cannot trigger this themselves; it is also called once
     * automatically right after the student submits, so the lecturer has results
     * waiting without needing to run it manually for every submission.
     */
    public SubmissionResponse runSubmission(UUID submissionId) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        Assignment assignment = submission.getAssignment();

        User user = currentUser();
        checkLecturerOwnsCourse(user, assignment);

        if (assignment.getAssignmentType() != AssignmentType.PROGRAMMING) {
            throw new RuntimeException("Only programming assignments can be run");
        }

        if (submission.getSourceCode() == null || submission.getSourceCode().isBlank()) {
            throw new RuntimeException(
                    "No readable source code found in this submission. " +
                    "Programming assignments must be submitted as a single plain-text source file " +
                    "(.java, .py, .c, .cpp)."
            );
        }

        runAndStore(submission, assignment);

        submission = submissionRepository.save(submission);

        return toResponse(submission, user);
    }

    /**
     * Runs every submission for a programming assignment in one go, so the
     * lecturer doesn't have to click "Run" on each row individually. Skips
     * submissions with no readable source code rather than failing the whole
     * batch, and reports per-submission outcomes so the lecturer can see
     * exactly what happened.
     */
    public BatchRunResponse runAllForAssignment(UUID assignmentId) {

        Assignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        User user = currentUser();
        checkLecturerOwnsCourse(user, assignment);

        if (assignment.getAssignmentType() != AssignmentType.PROGRAMMING) {
            throw new RuntimeException("Only programming assignments can be run");
        }

        List<Submission> submissions = submissionRepository
                .findByAssignmentIdOrderBySubmittedAtDesc(assignmentId);

        int ranCount = 0;
        int skippedCount = 0;
        int failedCount = 0;

        for (Submission submission : submissions) {
            if (submission.getSourceCode() == null || submission.getSourceCode().isBlank()) {
                skippedCount++;
                continue;
            }

            try {
                runAndStore(submission, assignment);
                submissionRepository.save(submission);
                ranCount++;
            } catch (Exception e) {
                // One submission's sandbox failure (e.g. a transient resource
                // issue) shouldn't stop the rest of the class from being graded.
                failedCount++;
            }
        }

        return BatchRunResponse.builder()
                .totalSubmissions(submissions.size())
                .ranCount(ranCount)
                .skippedCount(skippedCount)
                .failedCount(failedCount)
                .build();
    }

    /**
     * Asks the AI provider to estimate how likely this submission's text content
     * is AI-generated. Lecturer/admin only — this is a triage signal for the
     * lecturer to use their own judgement on, never an automatic penalty (see
     * AiService for the full reasoning and the disclaimer shown to the lecturer).
     */
    public SubmissionResponse analyzeForAi(UUID submissionId) {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        User viewer = currentUser();
        checkLecturerOwnsCourse(viewer, submission.getAssignment());

        String textToAnalyze = submission.getSourceCode();

        if (textToAnalyze == null || textToAnalyze.isBlank()) {
            throw new RuntimeException(
                    "AI analysis currently only supports plain-text submissions " +
                    "(.txt, .java, .py, .c, .cpp). This file type can't be analyzed yet."
            );
        }

        AiAnalysisResponse analysis = aiService.analyzeSubmissionText(textToAnalyze);

        submission.setAiLikelihoodPercent(analysis.getAiLikelihoodPercent());
        submission.setAiAnalysisExplanation(analysis.getExplanation());
        submission.setAiAnalyzedAt(analysis.getAnalyzedAt());

        submission = submissionRepository.save(submission);

        return toResponse(submission, viewer);
    }

    private void runAndStore(Submission submission, Assignment assignment) {

        List<TestCase> testCases = testCaseRepository
                .findByAssignmentIdOrderBySequenceAsc(assignment.getId());

        if (testCases.isEmpty()) {
            throw new RuntimeException("This assignment has no test cases configured yet");
        }

        ExecutionRunResult result = codeExecutionService.run(
                assignment.getProgrammingLanguage(),
                submission.getSourceCode(),
                testCases
        );

        try {
            submission.setLastRunResult(objectMapper.writeValueAsString(result));
        } catch (Exception e) {
            submission.setLastRunResult(null);
        }
        submission.setLastRunAt(LocalDateTime.now());

        // Auto-populate the score from the run, scaled to the assignment's max score,
        // UNLESS a lecturer has already manually graded this submission (manual grades win).
        boolean hasManualGrade = submission.getScore() != null && !submission.isAutoGraded();

        if (!hasManualGrade) {
            if (result.isCompiled() && result.getTotalPossible() > 0) {
                double scaled = (result.getTotalAwarded() / result.getTotalPossible()) * assignment.getMaxScore();
                scaled = applyLatePenalty(scaled, submission, assignment);
                submission.setScore(Math.round(scaled * 100.0) / 100.0);
                submission.setAutoGraded(true);
                submission.setGradedAt(LocalDateTime.now());
                submission.setGradedBy(null); // system-graded
            } else if (!result.isCompiled()) {
                submission.setScore(0.0);
                submission.setAutoGraded(true);
                submission.setGradedAt(LocalDateTime.now());
                submission.setGradedBy(null);
            }
        }
    }

    /**
     * Deducts assignment.latePenaltyPercent for each full day a submission was
     * late, floored at 0. A penalty of 0 (the default) is a no-op, so existing
     * assignments behave exactly as before unless a lecturer opts in.
     */
    private double applyLatePenalty(double rawScore, Submission submission, Assignment assignment) {

        if (assignment.getLatePenaltyPercent() <= 0 || assignment.getDueDate() == null) {
            return rawScore;
        }

        if (!submission.getSubmittedAt().isAfter(assignment.getDueDate())) {
            return rawScore;
        }

        long daysLate = java.time.Duration.between(assignment.getDueDate(), submission.getSubmittedAt()).toDays() + 1;
        double penaltyFraction = Math.min(1.0, (assignment.getLatePenaltyPercent() / 100.0) * daysLate);

        return rawScore * (1.0 - penaltyFraction);
    }

    // ---- helpers -----------------------------------------------------------

    private String extractSourceCodeIfPossible(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) return null;

        int dot = filename.lastIndexOf('.');
        if (dot < 0) return null;

        String ext = filename.substring(dot + 1).toLowerCase();
        if (!CODE_EXTENSIONS.contains(ext)) return null;

        // Cap how much we read into memory/DB — a 20MB "source file" is not legitimate.
        if (file.getSize() > 2 * 1024 * 1024) return null;

        try {
            byte[] bytes = file.getBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
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

    private void checkLecturerOwnsCourse(User user, Assignment assignment) {

        boolean isLecturer = user.getRole().name().equals("LECTURER");

        if (isLecturer && !assignment.getCourse().getLecturer().getId().equals(user.getId())) {
            throw new RuntimeException("You are not assigned to this course");
        }
    }

    private String generateUrl(String objectName) {
        try {
            return publicMinioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket("smartlearning")
                            .object(objectName)
                            .method(Method.GET)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private SubmissionResponse toResponse(Submission submission, User viewer) {

        Assignment assignment = submission.getAssignment();

        boolean late = assignment.getDueDate() != null
                && submission.getSubmittedAt().isAfter(assignment.getDueDate());

        List<TestCaseResult> runResults = null;
        if (submission.getLastRunResult() != null) {
            try {
                ExecutionRunResult parsed = objectMapper.readValue(
                        submission.getLastRunResult(), ExecutionRunResult.class);
                runResults = parsed.getTestCaseResults();
            } catch (Exception ignored) {
                runResults = null;
            }
        }

        // Students can preview/download their own file, but the raw extracted
        // source text is only surfaced to staff — this keeps one student from
        // reading another's code through this endpoint, and keeps the response
        // smaller for students who don't need it.
        boolean includeSourceCode = isStaff(viewer);

        return SubmissionResponse.builder()
                .id(submission.getId())
                .assignmentId(assignment.getId())
                .assignmentTitle(assignment.getTitle())
                .studentId(submission.getStudent().getId())
                .studentName(
                        submission.getStudent().getFirstName() + " " +
                        submission.getStudent().getLastName()
                )
                .fileName(submission.getFileName())
                .downloadUrl(generateUrl(submission.getObjectName()))
                .contentType(submission.getContentType())
                .fileSize(submission.getFileSize())
                .comment(submission.getComment())
                .reviewed(submission.isReviewed())
                .late(late)
                .submittedAt(submission.getSubmittedAt())
                .reviewedAt(submission.getReviewedAt())
                .score(submission.getScore())
                .maxScore(assignment.getMaxScore())
                .feedback(submission.getFeedback())
                .gradedAt(submission.getGradedAt())
                .gradedByName(submission.getGradedBy() != null
                        ? submission.getGradedBy().getFirstName() + " " + submission.getGradedBy().getLastName()
                        : (submission.isAutoGraded() ? "Automated grader" : null))
                .autoGraded(submission.isAutoGraded())
                .canRun(assignment.getAssignmentType() == AssignmentType.PROGRAMMING
                        && submission.getSourceCode() != null)
                .hasReadableText(submission.getSourceCode() != null && !submission.getSourceCode().isBlank())
                .sourceCode(includeSourceCode ? submission.getSourceCode() : null)
                .lastRunResult(runResults)
                .lastRunAt(submission.getLastRunAt())
                .aiLikelihoodPercent(submission.getAiLikelihoodPercent())
                .aiAnalysisExplanation(submission.getAiAnalysisExplanation())
                .aiAnalyzedAt(submission.getAiAnalyzedAt())
                .build();
    }

    private boolean isStaff(User user) {
        String role = user.getRole().name();
        return role.equals("LECTURER") || role.equals("ADMIN") || role.equals("MASTER");
    }
}
