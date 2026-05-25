package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.MembresiaInvitacion;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembresiaInvitacionRepository extends JpaRepository<MembresiaInvitacion, UUID> {

  @EntityGraph(attributePaths = "rol")
  List<MembresiaInvitacion> findAllByEmpresa_IdAndEstadoOrderByFechaCreacionDesc(
      UUID empresaId, String estado);

  @EntityGraph(attributePaths = "rol")
  List<MembresiaInvitacion> findAllByEmpresa_IdOrderByFechaCreacionDesc(UUID empresaId);

  @Query(
      "SELECT i FROM MembresiaInvitacion i "
          + "JOIN FETCH i.empresa "
          + "JOIN FETCH i.rol r "
          + "JOIN FETCH r.empresa "
          + "WHERE i.tokenHash = :hash AND i.estado = :estado AND i.expiresAt > :now")
  Optional<MembresiaInvitacion> findValidByTokenHash(
      @Param("hash") String hash, @Param("estado") String estado, @Param("now") Instant now);
}
