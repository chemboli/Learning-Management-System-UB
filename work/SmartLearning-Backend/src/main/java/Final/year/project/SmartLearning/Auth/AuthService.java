package Final.year.project.SmartLearning.Auth;

import Final.year.project.SmartLearning.dto.ActivateAccountRequest;
import Final.year.project.SmartLearning.dto.InvitationDetailsResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    /**
     * Looks up a pending invitation by its code and returns the identity
     * fields shown read-only on the activation page (name, email, …).
     * Throws if the code is unknown, already redeemed, or expired.
     */
    InvitationDetailsResponse getInvitationDetails(String code);

    /**
     * Redeems an invitation code: sets the password the user chose,
     * enables the account, and clears the code so it can't be reused.
     */
    void activateAccount(ActivateAccountRequest request);
}
