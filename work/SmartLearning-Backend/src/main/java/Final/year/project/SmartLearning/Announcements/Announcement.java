package Final.year.project.SmartLearning.Announcements;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(length = 8000, nullable = false)
    private String body;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AnnouncementPriority priority = AnnouncementPriority.NORMAL;

    /**
     * Null means this announcement is sitewide (visible to everyone).
     * Non-null scopes it to one course (visible to enrolled students + the lecturer).
     */
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private User author;

    @Builder.Default
    private boolean published = true;

    /** Optional — if set, the announcement is hidden from students after this time. */
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
