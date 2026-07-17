package Final.year.project.SmartLearning.Announcements;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    List<Announcement> findByCourseIsNullOrderByCreatedAtDesc();

    List<Announcement> findByCourseIdOrderByCreatedAtDesc(UUID courseId);

    List<Announcement> findByCourseIsNullAndPublishedTrueOrderByCreatedAtDesc();

    List<Announcement> findByCourseIdAndPublishedTrueOrderByCreatedAtDesc(UUID courseId);

    List<Announcement> findByAuthorIdOrderByCreatedAtDesc(UUID authorId);

    /** Sitewide + course-scoped announcements relevant to a set of enrolled course ids, published only. */
    List<Announcement> findByPublishedTrueAndCourseIdInOrderByCreatedAtDesc(List<UUID> courseIds);
}
