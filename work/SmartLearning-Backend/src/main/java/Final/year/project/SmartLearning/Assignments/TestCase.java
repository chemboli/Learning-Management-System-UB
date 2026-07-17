package Final.year.project.SmartLearning.Assignments;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "assignment_test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Order in which test cases are run / displayed. */
    private int sequence;

    /** Optional human-readable label, e.g. "Empty input" or "Large N". */
    private String label;

    @Column(length = 10000)
    private String input; // fed to the program's stdin

    @Column(length = 10000)
    private String expectedOutput; // compared against the program's stdout

    /** Marks awarded if this test case passes. */
    private double weight;

    /**
     * Hidden test cases are used for grading but never shown to students
     * (their input/expected output are withheld from student-facing responses).
     */
    private boolean hidden;

    @ManyToOne
    @JoinColumn(name = "assignment_id")
    private Assignment assignment;
}
