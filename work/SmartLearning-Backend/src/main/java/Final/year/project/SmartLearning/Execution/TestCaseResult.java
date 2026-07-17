package Final.year.project.SmartLearning.Execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {

    private UUID testCaseId;
    private String label;
    private boolean hidden;

    private String input;
    private String expectedOutput;
    private String actualOutput;

    private boolean passed;
    private double weight;
    private double awarded;

    /** COMPILE_ERROR | RUNTIME_ERROR | TIMEOUT | OK */
    private String status;

    private String errorOutput;

    private long executionTimeMs;
}
