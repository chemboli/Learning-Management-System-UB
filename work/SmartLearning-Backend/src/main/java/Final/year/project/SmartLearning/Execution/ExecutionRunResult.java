package Final.year.project.SmartLearning.Execution;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRunResult {

    /** Whether the code compiled (always true for interpreted languages with no syntax issues found). */
    private boolean compiled;

    private String compileError;

    private List<TestCaseResult> testCaseResults;

    private double totalAwarded;

    private double totalPossible;

    private int passedCount;

    private int totalCount;
}
