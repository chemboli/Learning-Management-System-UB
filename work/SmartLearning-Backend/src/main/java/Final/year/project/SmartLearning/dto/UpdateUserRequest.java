package Final.year.project.SmartLearning.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String firstName;

    private String lastName;

    private String matricule;

    private Boolean enabled;
}