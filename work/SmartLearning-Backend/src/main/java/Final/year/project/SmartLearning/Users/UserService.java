package Final.year.project.SmartLearning.Users;

import Final.year.project.SmartLearning.dto.AdminCreateUserRequest;
import Final.year.project.SmartLearning.dto.ChangePasswordRequest;
import Final.year.project.SmartLearning.dto.UpdateUserRequest;
import Final.year.project.SmartLearning.dto.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getUser(UUID id);

    UserResponse getUserByEmail(String email);

    List<UserResponse> getAllUsers();

    /**
     * Deletes a user. ADMIN may delete STUDENT/LECTURER accounts; deleting an
     * ADMIN or MASTER account is reserved for MASTER. callerRole is the role
     * of whoever is making this request — passed in because Spring's
     * @PreAuthorize can only see the caller's own role, not the role of the
     * user being acted on.
     */
    void deleteUser(UUID id, Role callerRole);

    UserResponse createUserByAdmin(AdminCreateUserRequest request);

    /**
     * Rotates the invitation code of a still-pending account and re-sends
     * the invitation email. Fails if the account is already activated.
     */
    void resendInvitation(UUID id);

    void changePassword(String email, ChangePasswordRequest request);
    UserResponse updateUser(String email, UpdateUserRequest request);
    List<UserResponse> getUsersByRole(Role role);

    /** CSV export, optionally filtered to one role. Pass null for all users. */
    String exportUsersCsv(Role roleFilter);
}