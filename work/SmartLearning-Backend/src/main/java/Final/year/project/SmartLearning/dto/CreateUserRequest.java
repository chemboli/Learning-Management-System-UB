package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Users.Role;
import lombok.Data;

@Data
public class CreateUserRequest {

    private String firstName;

    private String lastName;

    private String email;

    private String matricule;

    private String password;

    private Role role;
}