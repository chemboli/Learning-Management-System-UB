package Final.year.project.SmartLearning.Auth;

import Final.year.project.SmartLearning.Users.Role;
import Final.year.project.SmartLearning.Users.User;
import Final.year.project.SmartLearning.Users.UserRepository;
import Final.year.project.SmartLearning.dto.ActivateAccountRequest;
import Final.year.project.SmartLearning.dto.InvitationDetailsResponse;
import Final.year.project.SmartLearning.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already in use");
        }

        if (request.getMatricule() != null &&
                userRepository.findByMatricule(request.getMatricule()).isPresent()) {
            throw new RuntimeException("Matricule already in use");
        }

        // Self-registered users are always STUDENT by default
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .matricule(request.getMatricule())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.STUDENT)
                .enabled(true)
                .mustChangePassword(false)
                .build();

        userRepository.save(user);

        String token = jwtUtils.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .mustChangePassword(false)
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // A pending (not-yet-activated) account has a random placeholder
        // password, so authentication would fail anyway — but with a
        // confusing "bad credentials" error. Give a clear message instead.
        userRepository.findByEmail(request.getEmail())
                .filter(User::isPendingActivation)
                .ifPresent(u -> {
                    throw new RuntimeException(
                            "This account has not been activated yet. " +
                            "Please use the invitation code sent to your email.");
                });

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user.getEmail());

        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole().name())
                .mustChangePassword(user.isMustChangePassword())
                .build();
    }

    @Override
    public InvitationDetailsResponse getInvitationDetails(String code) {

        User user = findValidInvitation(code);

        return InvitationDetailsResponse.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .matricule(user.getMatricule())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public void activateAccount(ActivateAccountRequest request) {

        User user = findValidInvitation(request.getInvitationCode());

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(true);
        user.setMustChangePassword(false);
        // One-time use: clearing the code both marks the account activated
        // and makes the emailed link/code useless from now on.
        user.setInvitationCode(null);
        user.setInvitationExpiresAt(null);

        userRepository.save(user);
    }

    /** Shared lookup + validation for both invitation endpoints. */
    private User findValidInvitation(String code) {

        if (code == null || code.isBlank()) {
            throw new RuntimeException("Invitation code is required");
        }

        User user = userRepository.findByInvitationCode(code.trim().toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "Invalid invitation code. Check your email and try again."));

        if (user.getInvitationExpiresAt() != null
                && user.getInvitationExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException(
                    "This invitation has expired. Ask your administrator to send a new one.");
        }

        return user;
    }
}
