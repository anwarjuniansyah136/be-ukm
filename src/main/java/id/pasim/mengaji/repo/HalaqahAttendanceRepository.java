package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.HalaqahAttendance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HalaqahAttendanceRepository extends JpaRepository<HalaqahAttendance, UUID> {
    Optional<HalaqahAttendance> findBySession_IdAndUser_Id(UUID sessionId, UUID userId);
    List<HalaqahAttendance> findByUser_IdOrderByCreatedAtDesc(UUID userId);
    List<HalaqahAttendance> findBySession_IdOrderByCreatedAtAsc(UUID sessionId);
    long countBySession_IdAndCompletedTrue(UUID sessionId);
}
