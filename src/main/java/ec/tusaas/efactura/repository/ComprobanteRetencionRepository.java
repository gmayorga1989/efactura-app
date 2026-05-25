package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ComprobanteRetencion;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteRetencionRepository extends JpaRepository<ComprobanteRetencion, UUID> {

  List<ComprobanteRetencion> findByComprobante_Id(UUID comprobanteId);

  @Query(
      "select coalesce(sum(r.baseImponible), 0) from ComprobanteRetencion r "
          + "where r.comprobante.empresa.id = :empresaId and r.comprobante.estado <> 'ELIMINADO' "
          + "and r.comprobante.tipoCodigo = '07' and r.comprobante.fechaEmision between :desde and :hasta "
          + "and r.estado <> 'ELIMINADO'")
  BigDecimal sumarBaseImponible(
      @Param("empresaId") UUID empresaId,
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta);

  @Query(
      "select coalesce(sum(r.valor), 0) from ComprobanteRetencion r "
          + "where r.comprobante.empresa.id = :empresaId and r.comprobante.estado <> 'ELIMINADO' "
          + "and r.comprobante.tipoCodigo = '07' and r.comprobante.fechaEmision between :desde and :hasta "
          + "and r.estado <> 'ELIMINADO'")
  BigDecimal sumarValorRetenido(
      @Param("empresaId") UUID empresaId,
      @Param("desde") java.time.LocalDate desde,
      @Param("hasta") java.time.LocalDate hasta);
}
