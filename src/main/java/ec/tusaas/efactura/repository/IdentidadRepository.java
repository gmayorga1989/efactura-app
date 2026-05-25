package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Identidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IdentidadRepository extends JpaRepository<Identidad, UUID> {

  Optional<Identidad> findByEmailIgnoreCase(String email);

  @Query(
      "SELECT DISTINCT i FROM Identidad i "
          + "LEFT JOIN FETCH i.membresias m "
          + "LEFT JOIN FETCH m.empresa e "
          + "LEFT JOIN FETCH m.roles r "
          + "LEFT JOIN FETCH r.permisos "
          + "WHERE i.id = :id")
  Optional<Identidad> findByIdWithMembresiasAndRoles(@Param("id") UUID id);
}
