package Final.year.project.SmartLearning.Assignments;

import Final.year.project.SmartLearning.Courses.Course;
import Final.year.project.SmartLearning.Submissions.Submission;
import Final.year.project.SmartLearning.Users.User;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String title;

    @Column(length = 5000)
    private String description;

    private LocalDateTime dueDate;

    private boolean published;

    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AssignmentType assignmentType = AssignmentType.REGULAR;

    /** Only relevant when assignmentType == PROGRAMMING. */
    @Enumerated(EnumType.STRING)
    private ProgrammingLanguage programmingLanguage;

    /** Maximum score for this assignment, used for both manual and automated grading. */
    @Builder.Default
    private double maxScore = 100.0;

    /**
     * Percentage deducted from the score for each full day a submission is
     * late, e.g. 10 means 10% off per day late. 0 (the default) means no
     * penalty is applied — existing assignments are unaffected unless a
     * lecturer explicitly sets this.
     */
    @Builder.Default
    private double latePenaltyPercent = 0.0;

    @ManyToOne
    private Course course;

    @ManyToOne
    private User lecturer;

    @OneToMany(
            mappedBy = "assignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Submission> submissions = new ArrayList<>();

    @OneToMany(
            mappedBy = "assignment",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<TestCase> testCases = new ArrayList<>();
}
