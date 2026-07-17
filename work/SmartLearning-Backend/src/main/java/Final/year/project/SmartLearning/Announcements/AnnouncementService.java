package Final.year.project.SmartLearning.Announcements;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Courses.CourseRepository;
import Final.year.project.SmartLearning.Enrollments.Enrollment;
import Final.year.project.SmartLearning.Enrollments.EnrollmentRepository;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.AnnouncementResponse;
import Final.year.project.SmartLearning.dto.CreateAnnouncementRequest;
import Final.year.project.SmartLearning.dto.UpdateAnnouncementRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public AnnouncementResponse create(CreateAnnouncementRequest request) {

        User author = currentUser();

        Course course = null;
        if (request.getCourseId() != null) {
            course = courseRepository.findById(request.getCourseId())
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            checkLecturerOwnsCourse(author, course);
        } else {
            // Only admins/master can post sitewide announcements.
            boolean privileged = author.getRole() == Final.year.project.SmartLearning.Users.Role.ADMIN
                    || author.getRole() == Final.year.project.SmartLearning.Users.Role.MASTER;
            if (!privileged) {
                throw new RuntimeException("Only administrators can post sitewide announcements");
            }
        }

        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .priority(parsePriority(request.getPriority()))
                .course(course)
                .author(author)
                .published(request.isPublished())
                .expiresAt(request.getExpiresAt())
                .build();

        announcement = announcementRepository.save(announcement);

        return toResponse(announcement);
    }

    /** Sitewide announcements only. */
    public List<AnnouncementResponse> getSitewide() {

        User user = currentUser();
        boolean isStudent = user.getRole().name().equals("STUDENT");

        List<Announcement> announcements = isStudent
                ? announcementRepository.findByCourseIsNullAndPublishedTrueOrderByCreatedAtDesc()
                : announcementRepository.findByCourseIsNullOrderByCreatedAtDesc();

        return filterExpiredForStudents(announcements, isStudent).stream()
                .map(this::toResponse)
                .toList();
    }

    /** Announcements scoped to one course. */
    public List<AnnouncementResponse> getForCourse(UUID courseId) {

        User user = currentUser();
        boolean isStudent = user.getRole().name().equals("STUDENT");

        List<Announcement> announcements = isStudent
                ? announcementRepository.findByCourseIdAndPublishedTrueOrderByCreatedAtDesc(courseId)
                : announcementRepository.findByCourseIdOrderByCreatedAtDesc(courseId);

        return filterExpiredForStudents(announcements, isStudent).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Personalized feed for the current user: sitewide announcements plus
     * announcements from every course they're enrolled in (students) or teach
     * (lecturers). Used for the dashboard / notification bell.
     */
    public List<AnnouncementResponse> getFeedForCurrentUser() {

        User user = currentUser();
        String role = user.getRole().name();

        List<Announcement> result = new java.util.ArrayList<>(
                role.equals("STUDENT")
                        ? announcementRepository.findByCourseIsNullAndPublishedTrueOrderByCreatedAtDesc()
                        : announcementRepository.findByCourseIsNullOrderByCreatedAtDesc()
        );

        List<UUID> courseIds;
        if (role.equals("STUDENT")) {
            courseIds = enrollmentRepository.findByStudentId(user.getId()).stream()
                    .map(Enrollment::getCourse)
                    .map(Course::getId)
                    .toList();

            if (!courseIds.isEmpty()) {
                result.addAll(
                        announcementRepository.findByPublishedTrueAndCourseIdInOrderByCreatedAtDesc(courseIds)
                );
            }
        } else if (role.equals("LECTURER")) {
            // Lecturers see announcements for courses they teach, published or not (so they can manage drafts).
            List<Announcement> own = announcementRepository.findByAuthorIdOrderByCreatedAtDesc(user.getId());
            result.addAll(own);
        }

        boolean isStudent = role.equals("STUDENT");

        return filterExpiredForStudents(result, isStudent).stream()
                .distinct()
                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    public AnnouncementResponse update(UUID id, UpdateAnnouncementRequest request) {

        User user = currentUser();

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        checkCanManage(user, announcement);

        if (request.getTitle() != null) announcement.setTitle(request.getTitle());
        if (request.getBody() != null) announcement.setBody(request.getBody());
        if (request.getPriority() != null) announcement.setPriority(parsePriority(request.getPriority()));
        if (request.getPublished() != null) announcement.setPublished(request.getPublished());
        if (request.getExpiresAt() != null) announcement.setExpiresAt(request.getExpiresAt());

        announcement = announcementRepository.save(announcement);

        return toResponse(announcement);
    }

    public void delete(UUID id) {

        User user = currentUser();

        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Announcement not found"));

        checkCanManage(user, announcement);

        announcementRepository.delete(announcement);
    }

    // ---- helpers -----------------------------------------------------------

    private List<Announcement> filterExpiredForStudents(List<Announcement> announcements, boolean isStudent) {
        if (!isStudent) return announcements;
        LocalDateTime now = LocalDateTime.now();
        return announcements.stream()
                .filter(a -> a.getExpiresAt() == null || a.getExpiresAt().isAfter(now))
                .toList();
    }

    private AnnouncementPriority parsePriority(String value) {
        if (value == null || value.isBlank()) return AnnouncementPriority.NORMAL;
        try {
            return AnnouncementPriority.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid priority: " + value);
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    private void checkLecturerOwnsCourse(User user, Course course) {
        boolean isLecturer = user.getRole().name().equals("LECTURER");
        if (isLecturer && !course.getLecturer().getId().equals(user.getId())) {
            throw new RuntimeException("You are not assigned to this course");
        }
    }

    private void checkCanManage(User user, Announcement announcement) {
        boolean isPrivileged = user.getRole().name().equals("ADMIN") || user.getRole().name().equals("MASTER");
        boolean isAuthor = announcement.getAuthor() != null && announcement.getAuthor().getId().equals(user.getId());

        if (!isPrivileged && !isAuthor) {
            throw new RuntimeException("You do not have permission to manage this announcement");
        }
    }

    private AnnouncementResponse toResponse(Announcement announcement) {

        boolean expired = announcement.getExpiresAt() != null
                && LocalDateTime.now().isAfter(announcement.getExpiresAt());

        AnnouncementResponse.AnnouncementResponseBuilder builder = AnnouncementResponse.builder()
                .id(announcement.getId())
                .title(announcement.getTitle())
                .body(announcement.getBody())
                .priority(announcement.getPriority().name())
                .sitewide(announcement.getCourse() == null)
                .authorName(announcement.getAuthor() != null
                        ? announcement.getAuthor().getFirstName() + " " + announcement.getAuthor().getLastName()
                        : "System")
                .published(announcement.isPublished())
                .expired(expired)
                .expiresAt(announcement.getExpiresAt())
                .createdAt(announcement.getCreatedAt())
                .updatedAt(announcement.getUpdatedAt());

        if (announcement.getCourse() != null) {
            builder.courseId(announcement.getCourse().getId())
                    .courseCode(announcement.getCourse().getCourseCode())
                    .courseTitle(announcement.getCourse().getTitle());
        }

        return builder.build();
    }
}
