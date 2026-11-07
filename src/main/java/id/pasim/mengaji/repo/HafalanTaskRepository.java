package id.pasim.mengaji.repo;
import id.pasim.mengaji.domain.HafalanTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface HafalanTaskRepository extends JpaRepository<HafalanTask,UUID> { @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE) @org.springframework.data.jpa.repository.Query("select t from HafalanTask t where t.id = :id") Optional<HafalanTask> lockById(@org.springframework.data.repository.query.Param("id") UUID id); List<HafalanTask> findAllByOrderByDueDateAsc(); List<HafalanTask> findByMember_IdOrderByDueDateAsc(UUID memberId); }
