package Final.year.project.SmartLearning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class TestCaseDto {

    private UUID id;
    private int sequence;
    private String label;
    private String input;
    private String expectedOutput;
    private double weight;
    private boolean hidden;
}
