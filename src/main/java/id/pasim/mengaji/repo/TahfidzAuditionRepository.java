package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.AuditionStatus;
import id.pasim.mengaji.domain.TahfidzAudition;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TahfidzAuditionRepository extends JpaRepository<TahfidzAudition, UUID> {
    List<TahfidzAudition> findAllByOrderByCreatedAtDesc();
    Optional<TahfidzAudition> findFirstByMember_IdOrderByCreatedAtDesc(UUID memberId);
    List<TahfidzAudition> findByStatusOrderByCreatedAtDesc(AuditionStatus status);
    List<TahfidzAudition> findByTargetApprovalStatusOrderByCreatedAtDesc(AuditionStatus status);
}
