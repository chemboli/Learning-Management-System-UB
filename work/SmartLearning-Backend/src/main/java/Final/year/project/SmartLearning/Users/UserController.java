package Final.year.project.SmartLearning.Users;

import Final.year.project.SmartLearning.Users.UserService;
import Final.year.project.SmartLearning.dto.AdminCreateUserRequest;
import Final.year.project.SmartLearning.dto.ChangePasswordRequest;
import Final.year.project.SmartLearning.dto.UpdateUserRequest;
import Final.year.project.SmartLearning.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse getCurrentUser() {

        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userService.getUserByEmail(email);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public List<UserResponse> getAllUsers() {

        return userService.getAllUsers();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public UserResponse getUser(
            @PathVariable UUID id) {

        return userService.getUser(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public void deleteUser(
            @PathVariable UUID id) {

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        Role callerRole = userService.getUserByEmail(email).getRole();

        userService.deleteUser(id, callerRole);
    }

    @PostMapping("/admin/create")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public UserResponse createUserByAdmin(
            @RequestBody @Valid AdminCreateUserRequest request) {

        return userService.createUserByAdmin(request);
    }

    /**
     * Rotates the invitation code of a still-pending account and re-sends
     * the invitation email. Useful when the original email was lost or the
     * code expired. Admin/master only.
     */
    @PostMapping("/{id}/resend-invitation")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public String resendInvitation(
            @PathVariable UUID id) {

        userService.resendInvitation(id);

        return "Invitation re-sent";
    }

    @PutMapping("/change-password/{email}")
    public String changePassword(
            @PathVariable String email,
            @RequestBody @Valid ChangePasswordRequest request) {

        userService.changePassword(email, request);

        return "Password changed successfully";
    }
    @PutMapping("/{email}")
    public UserResponse updateUser(
            @PathVariable String email,
            @RequestBody UpdateUserRequest request) {

        return userService.updateUser(email, request);
    }
    @GetMapping("/students")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public List<UserResponse> getStudents() {

        return userService.getUsersByRole(Role.STUDENT);
    }
    @GetMapping("/lecturers")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public List<UserResponse> getLecturers() {

        return userService.getUsersByRole(Role.LECTURER);
    }

    /**
     * Downloads a CSV of users. Pass ?role=STUDENT (or LECTURER/ADMIN/MASTER) to
     * filter; omit it to export everyone. Admin/master only.
     */
    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('MASTER','ADMIN')")
    public ResponseEntity<byte[]> exportUsers(
            @RequestParam(required = false) Role role) {

        String csv = userService.exportUsersCsv(role);
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);

        String filename = role != null
                ? "users-" + role.name().toLowerCase() + ".csv"
                : "users-all.csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

}