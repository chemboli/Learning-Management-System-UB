package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BatchRunResponse {

    private int totalSubmissions;

    private int ranCount;

    /** Submissions with no readable source code to execute. */
    private int skippedCount;

    /** Submissions that threw during execution (sandbox error, etc.) rather than just failing test cases. */
    private int failedCount;
}
