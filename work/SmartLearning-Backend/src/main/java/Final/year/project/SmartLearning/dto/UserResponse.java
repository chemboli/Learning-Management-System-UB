package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Users.Role;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserResponse {

    private UUID id;

    private String firstName;

    private String lastName;

    private String email;

    private String matricule;

    private Role role;

    private boolean enabled;

    /**
     * True when the user must change their password on next login
     * (i.e. the account was created by an admin and the user
     * has not yet set a personal password).
     */
    private boolean mustChangePassword;

    /**
     * True while the account was created by an admin but the user has not
     * yet redeemed their invitation code and chosen a password.
     */
    private boolean pendingActivation;
}
