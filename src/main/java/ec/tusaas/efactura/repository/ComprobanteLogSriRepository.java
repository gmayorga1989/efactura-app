package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ComprobanteLogSri;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteLogSriRepository extends JpaRepository<ComprobanteLogSri, UUID> {

  Optional<ComprobanteLogSri> findFirstByComprobante_IdOrderByFechaDesc(UUID comprobanteId);

  List<ComprobanteLogSri> findByComprobante_IdOrderByFechaAsc(UUID comprobanteId);

  @Query(
      "SELECT l FROM ComprobanteLogSri l JOIN FETCH l.comprobante c "
          + "WHERE l.empresa.id = :eid AND c.fechaEmision BETWEEN :d1 AND :d2 "
          + "ORDER BY l.fecha DESC")
  Page<ComprobanteLogSri> findHistorialEmpresa(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate fechaDesde,
      @Param("d2") LocalDate fechaHasta,
      Pageable pageable);
}
