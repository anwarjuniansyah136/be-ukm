package id.pasim.mengaji.repo;

import id.pasim.mengaji.domain.Activity;
import id.pasim.mengaji.domain.ActivityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ActivityRepository extends JpaRepository<Activity, UUID> {
    List<Activity> findAllByOrderByEventDateAsc();
    List<Activity> findByStatusOrderByEventDateAsc(ActivityStatus status);
}
