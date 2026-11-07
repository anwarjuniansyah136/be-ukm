package id.pasim.mengaji.repo;
import id.pasim.mengaji.domain.Material;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface MaterialRepository extends JpaRepository<Material,UUID> { List<Material> findAllByOrderByCreatedAtDesc(); }
