package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AiAnalysisResponse {

    /** 0-100 — the model's estimate of how likely the text is AI-generated. NOT proof. */
    private int aiLikelihoodPercent;

    /** Short plain-language explanation of what drove the estimate. */
    private String explanation;

    /** Always shown alongside the score so it can't be read as a verdict. */
    private String disclaimer;

    private LocalDateTime analyzedAt;
}
