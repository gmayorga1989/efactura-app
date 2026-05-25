package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Secuencial;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecuencialRepository extends JpaRepository<Secuencial, UUID> {

  List<Secuencial> findByPuntoEmision_IdOrderByTipoComprobanteAsc(UUID puntoEmisionId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "SELECT s FROM Secuencial s JOIN FETCH s.empresa JOIN FETCH s.puntoEmision "
          + "WHERE s.puntoEmision.id = :puntoEmisionId AND s.tipoComprobante = :tipo")
  Optional<Secuencial> findForUpdate(
      @Param("puntoEmisionId") UUID puntoEmisionId, @Param("tipo") String tipo);

  Optional<Secuencial> findByPuntoEmision_IdAndTipoComprobante(UUID puntoEmisionId, String tipo);
}
