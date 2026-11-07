package id.pasim.mengaji.repo;
import id.pasim.mengaji.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface UserRepository extends JpaRepository<UserAccount,UUID> { Optional<UserAccount> findByEmail(String email); boolean existsByEmail(String email); List<UserAccount> findAllByOrderByCreatedAtDesc(); List<UserAccount> findByStatusOrderByCreatedAtDesc(RegistrationStatus status); }
