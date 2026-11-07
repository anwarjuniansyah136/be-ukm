package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.Program;
import id.pasim.mengaji.domain.ProgramType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface ProgramRepository extends JpaRepository<Program, UUID> {
    List<Program> findAllByOrderByCreatedAtDesc();
    List<Program> findByTypeAndActiveTrue(ProgramType type);
}
