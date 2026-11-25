package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.TahlilTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TahlilTaskRepository extends JpaRepository<TahlilTask, UUID> {
    List<TahlilTask> findAllByOrderByCreatedAtDesc();
    List<TahlilTask> findByMemberIsNullOrMember_IdOrderByCreatedAtDesc(UUID memberId);
}
