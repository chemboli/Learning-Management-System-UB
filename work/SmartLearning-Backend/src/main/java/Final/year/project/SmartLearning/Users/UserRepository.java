package Final.year.project.SmartLearning.Users;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByMatricule(String matricule);

    Optional<User> findByInvitationCode(String invitationCode);

    boolean existsByEmail(String email);
    List<User> findByRole(Role role);

    boolean existsByMatricule(String matricule);
}