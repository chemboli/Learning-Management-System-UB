package Final.year.project.SmartLearning.Users;

import Final.year.project.SmartLearning.dto.AdminCreateUserRequest;
import Final.year.project.SmartLearning.dto.ChangePasswordRequest;
import Final.year.project.SmartLearning.dto.UpdateUserRequest;
import Final.year.project.SmartLearning.dto.UserResponse;
import Final.year.project.SmartLearning.shared.CsvWriter;
import Final.year.project.SmartLearning.shared.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Value("${app.invitation-expiry-days:7}")
    private long invitationExpiryDays;

    @Override
    public UserResponse getUser(UUID id) {
        return userRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public UserResponse getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::mapToResponse)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Override
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public void deleteUser(UUID id, Role callerRole) {

        User target = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean targetIsPrivileged = target.getRole() == Role.ADMIN || target.getRole() == Role.MASTER;

        if (targetIsPrivileged && callerRole != Role.MASTER) {
            throw new RuntimeException(
                    "Only a MASTER account can delete an ADMIN or MASTER account."
            );
        }

        userRepository.deleteById(id);
    }

    @Override
    public UserResponse createUserByAdmin(AdminCreateUserRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        boolean isStudent = request.getRole() == Role.STUDENT;

        if (isStudent) {
            if (request.getMatricule() == null || request.getMatricule().isBlank()) {
                throw new RuntimeException("Matricule is required for students");
            }
            if (userRepository.findByMatricule(request.getMatricule()).isPresent()) {
                throw new RuntimeException("Matricule already exists");
            }
        }

        // Invitation flow: the admin never sets a password. The account is
        // created disabled with a random unusable placeholder password and a
        // one-time invitation code. The user redeems the code on the portal's
        // activation page, where they choose their own password.
        String invitationCode = generateInvitationCode();

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .matricule(isStudent ? request.getMatricule() : null)
                // Random placeholder — nobody knows it, so login is impossible
                // until the user activates and sets a real password.
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .role(request.getRole())
                .enabled(false)                      // enabled on activation
                .mustChangePassword(false)           // they choose their own password
                .invitationCode(invitationCode)
                .invitationExpiresAt(Instant.now().plus(invitationExpiryDays, ChronoUnit.DAYS))
                .build();

        user = userRepository.save(user);

        // Send the invitation email with the portal link + code
        // (best-effort, non-blocking — can be re-sent from the user list).
        emailService.sendInvitationEmail(
                user.getEmail(),
                user.getFirstName(),
                user.getInvitationCode(),
                user.getInvitationExpiresAt()
        );

        return mapToResponse(user);
    }

    @Override
    public void resendInvitation(UUID id) {

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isPendingActivation()) {
            throw new RuntimeException("This account has already been activated");
        }

        // Rotate the code and reset the expiry window so old emails go stale.
        user.setInvitationCode(generateInvitationCode());
        user.setInvitationExpiresAt(Instant.now().plus(invitationExpiryDays, ChronoUnit.DAYS));
        user = userRepository.save(user);

        emailService.sendInvitationEmail(
                user.getEmail(),
                user.getFirstName(),
                user.getInvitationCode(),
                user.getInvitationExpiresAt()
        );
    }

    /**
     * Human-friendly 8-character code from an unambiguous alphabet
     * (no 0/O, 1/I/L) — easy to read from an email and type into the portal.
     * Uniqueness is enforced by the DB unique constraint; collisions are
     * astronomically unlikely (30^8) but we retry defensively anyway.
     */
    private String generateInvitationCode() {
        final String alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ2345679";
        for (int attempt = 0; attempt < 5; attempt++) {
            StringBuilder sb = new StringBuilder(8);
            for (int i = 0; i < 8; i++) {
                sb.append(alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length())));
            }
            String code = sb.toString();
            if (userRepository.findByInvitationCode(code).isEmpty()) {
                return code;
            }
        }
        throw new RuntimeException("Could not generate a unique invitation code, please retry");
    }

    @Override
    public void changePassword(String email, ChangePasswordRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // If the user is in "must change password" mode (first login after admin
        // creation), skip verifying the old/temporary password — they may not know it.
        if (!user.isMustChangePassword()) {
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new RuntimeException("Current password is incorrect");
            }
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);  // clear the forced-change flag

        userRepository.save(user);
    }

    @Override
    public UserResponse updateUser(String email, UpdateUserRequest request) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getMatricule() != null) {
            user.setMatricule(request.getMatricule());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        user = userRepository.save(user);

        return mapToResponse(user);
    }

    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .matricule(user.getMatricule())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .mustChangePassword(user.isMustChangePassword())
                .pendingActivation(user.isPendingActivation())
                .build();
    }

    @Override
    public List<UserResponse> getUsersByRole(Role role) {

        return userRepository.findByRole(role)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public String exportUsersCsv(Role roleFilter) {

        List<User> users = roleFilter != null
                ? userRepository.findByRole(roleFilter)
                : userRepository.findAll();

        List<String> header = List.of(
                "First Name", "Last Name", "Email", "Matricule", "Role", "Enabled"
        );

        List<List<String>> rows = users.stream()
                .map(u -> List.of(
                        u.getFirstName(),
                        u.getLastName(),
                        u.getEmail(),
                        u.getMatricule() != null ? u.getMatricule() : "",
                        u.getRole().name(),
                        u.isEnabled() ? "Yes" : "No"
                ))
                .toList();

        return CsvWriter.write(header, rows);
    }
}
