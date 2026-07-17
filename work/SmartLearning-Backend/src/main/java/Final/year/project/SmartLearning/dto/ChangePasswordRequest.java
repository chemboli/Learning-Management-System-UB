package Final.year.project.SmartLearning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    /**
     * The user's current (or temporary) password.
     * Not required when the account is in "mustChangePassword" mode
     * (first login after admin creation) — in that case the backend
     * skips verification of this field.
     */
    private String currentPassword;

    @NotBlank
    private String newPassword;
}
