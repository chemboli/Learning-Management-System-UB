package Final.year.project.SmartLearning.Auth;

import lombok.Data;

@Data
public class RegisterRequest {

    private String firstName;

    private String lastName;

    private String email;

    private String matricule;

    private String password;
}