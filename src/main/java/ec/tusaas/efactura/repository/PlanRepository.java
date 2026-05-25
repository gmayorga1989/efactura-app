package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Plan;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, UUID> {

  Optional<Plan> findByCodigo(String codigo);
}
