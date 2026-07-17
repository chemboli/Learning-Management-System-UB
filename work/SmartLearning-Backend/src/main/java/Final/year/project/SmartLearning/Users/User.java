package Final.year.project.SmartLearning.Users;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    //@Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    //@Column(nullable = false, unique = true)
    private String matricule;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * When true the user must change their password on next login.
     * Set to true for accounts created by an admin; cleared to false
     * once the user successfully changes their password.
     */
    @Column(nullable = false)
    private boolean mustChangePassword = false;

    /**
     * One-time invitation code sent by email when an admin creates the
     * account. While this is non-null the account is "pending activation":
     * the user cannot log in until they redeem the code on the portal's
     * activation page and choose their own password. Cleared on activation.
     */
    @Column(unique = true)
    private String invitationCode;

    /** When the invitation code stops being valid. Null once activated. */
    private java.time.Instant invitationExpiresAt;

    /** Convenience: true while the account still awaits activation. */
    @Transient
    public boolean isPendingActivation() {
        return invitationCode != null;
    }
}
