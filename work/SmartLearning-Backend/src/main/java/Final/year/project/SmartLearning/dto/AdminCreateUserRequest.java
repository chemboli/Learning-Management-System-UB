package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Users.Role;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class AdminCreateUserRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    private String matricule;

    @NotNull
    private Role role;
}