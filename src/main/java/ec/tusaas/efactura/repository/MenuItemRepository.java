package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.MenuItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

  @Query(
      "SELECT m FROM MenuItem m LEFT JOIN FETCH m.padre WHERE m.estado = :estado ORDER BY m.orden ASC, m.etiqueta ASC")
  List<MenuItem> findAllActivosOrdered(@Param("estado") String estado);
}
