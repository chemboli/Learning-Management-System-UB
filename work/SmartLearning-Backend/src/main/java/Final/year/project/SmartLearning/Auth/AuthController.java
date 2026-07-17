package Final.year.project.SmartLearning.Auth;

import Final.year.project.SmartLearning.dto.ActivateAccountRequest;
import Final.year.project.SmartLearning.dto.InvitationDetailsResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody @Valid RegisterRequest request) {

        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody @Valid LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Public: the activation page calls this after the user enters their
     * invitation code, to load their name/email read-only. Returns 4xx-style
     * errors if the code is invalid, redeemed, or expired.
     */
    @GetMapping("/invitation/{code}")
    public ResponseEntity<InvitationDetailsResponse> getInvitation(
            @PathVariable String code) {

        return ResponseEntity.ok(authService.getInvitationDetails(code));
    }

    /**
     * Public: redeems the invitation code with the password the user chose.
     * After this succeeds the user signs in normally on the login page.
     */
    @PostMapping("/activate")
    public ResponseEntity<String> activate(
            @RequestBody @Valid ActivateAccountRequest request) {

        authService.activateAccount(request);

        return ResponseEntity.ok("Account activated successfully");
    }
}