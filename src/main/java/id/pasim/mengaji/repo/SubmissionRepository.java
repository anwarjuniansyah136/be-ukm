package id.pasim.mengaji.repo;
import id.pasim.mengaji.domain.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface SubmissionRepository extends JpaRepository<Submission,UUID> { List<Submission> findAllByOrderBySubmittedAtDesc(); List<Submission> findByMember_IdOrderBySubmittedAtDesc(UUID memberId); Optional<Submission> findFirstByTask_IdOrderBySubmittedAtDesc(UUID taskId); boolean existsByTask_IdAndMember_Id(UUID taskId,UUID memberId); }
