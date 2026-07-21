package Final.year.project.SmartLearning.Auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AuthResponse {

    private String token;

    private String email;

    private String firstName;

    private String lastName;

    private String role;

    /**
     * True when the account was created by an admin and the user
     * has not yet set a personal password. The frontend should
     * redirect to the change-password screen when this is true.
     */
    private boolean mustChangePassword;
}
