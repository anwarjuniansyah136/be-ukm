package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.HalaqahSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface HalaqahSessionRepository extends JpaRepository<HalaqahSession, UUID> {
    List<HalaqahSession> findAllByOrderByScheduledDateDescCreatedAtDesc();
}
