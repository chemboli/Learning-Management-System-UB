package Final.year.project.SmartLearning.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Sent from the activation page: the invitation code the user received by
 * email plus the password they have chosen for their account.
 */
@Data
public class ActivateAccountRequest {

    @NotBlank
    private String invitationCode;

    @NotBlank
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
