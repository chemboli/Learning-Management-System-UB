package Final.year.project.SmartLearning.dto;

import Final.year.project.SmartLearning.Users.Role;
import lombok.Builder;
import lombok.Data;

/**
 * Returned when someone enters a valid invitation code on the activation
 * page. Contains only the identity fields the page displays read-only —
 * never anything sensitive.
 */
@Data
@Builder
public class InvitationDetailsResponse {

    private String firstName;

    private String lastName;

    private String email;

    private String matricule;

    private Role role;
}
