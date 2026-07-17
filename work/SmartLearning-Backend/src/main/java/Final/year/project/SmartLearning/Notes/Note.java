package Final.year.project.SmartLearning.Notes;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Users.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(length = 5000)
    private String description;

    private String fileName;

    private String objectName; // MinIO file path

    private String contentType;

    private long fileSize;

    @Enumerated(EnumType.STRING)
    private NoteType noteType;

    private Integer weekNumber;

    private boolean published;

    private LocalDateTime uploadedAt;

    @ManyToOne
    private Course course;

    @ManyToOne
    private User lecturer;
}