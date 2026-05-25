package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.dto.reporte.EstadoSriConteo;
import ec.tusaas.efactura.entity.Comprobante;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComprobanteRepository extends JpaRepository<Comprobante, UUID>, JpaSpecificationExecutor<Comprobante> {

  @EntityGraph(attributePaths = "empresa")
  Optional<Comprobante> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  Optional<Comprobante> findByEmpresa_IdAndIdempotencyKey(UUID empresaId, String idempotencyKey);

  Page<Comprobante> findByEmpresa_IdOrderByFechaCreacionDesc(UUID empresaId, Pageable pageable);

  Page<Comprobante> findByEmpresa_IdAndTipoOrderByFechaCreacionDesc(UUID empresaId, String tipo, Pageable pageable);

  List<Comprobante> findTop5ByEmpresa_IdAndEstadoNotOrderByFechaEmisionDescFechaCreacionDesc(
      UUID empresaId, String estado);

  long countByEmpresa_IdAndFechaEmisionBetween(UUID empresaId, LocalDate desde, LocalDate hasta);

  long countByEmpresa_IdAndEstadoNotAndFechaEmisionBetween(
      UUID empresaId, String estado, LocalDate desde, LocalDate hasta);

  long countByEmpresa_IdAndEstadoNotAndFechaEmisionBetweenAndEstadoSri(
      UUID empresaId, String estado, LocalDate desde, LocalDate hasta, String estadoSri);

  @Query(
      "select coalesce(sum(c.valorTotal), 0) from Comprobante c "
          + "where c.empresa.id = :eid and c.estado <> 'ELIMINADO' "
          + "and c.fechaEmision between :d1 and :d2 and upper(c.tipo) = upper(:tipo)")
  BigDecimal sumarTotalPorTipo(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate desde,
      @Param("d2") LocalDate hasta,
      @Param("tipo") String tipo);

  @Query(
      "select coalesce(sum(c.valorTotal), 0) from Comprobante c "
          + "where c.empresa.id = :eid and c.estado <> 'ELIMINADO' "
          + "and c.fechaEmision between :d1 and :d2 and c.tipoCodigo = :tipoCodigo")
  BigDecimal sumarTotalPorTipoCodigo(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate desde,
      @Param("d2") LocalDate hasta,
      @Param("tipoCodigo") String tipoCodigo);

  @Query(
      "select new ec.tusaas.efactura.dto.dashboard.DashboardSerieDiaResponse("
          + "c.fechaEmision, coalesce(sum(c.valorTotal), 0), count(c)) "
          + "from Comprobante c where c.empresa.id = :eid and c.estado <> 'ELIMINADO' "
          + "and c.fechaEmision between :d1 and :d2 and upper(c.tipo) = upper(:tipo) "
          + "group by c.fechaEmision order by c.fechaEmision asc")
  List<ec.tusaas.efactura.dto.dashboard.DashboardSerieDiaResponse> ventasPorDia(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate desde,
      @Param("d2") LocalDate hasta,
      @Param("tipo") String tipo);

  long countByEmpresa_IdAndEstablecimientoCodigo(UUID empresaId, String establecimientoCodigo);

  long countByEmpresa_IdAndEstablecimientoCodigoAndPuntoEmisionCodigo(
      UUID empresaId, String establecimientoCodigo, String puntoEmisionCodigo);

  @Query(
      "select new ec.tusaas.efactura.dto.reporte.EstadoSriConteo(c.estadoSri, count(c)) "
          + "from Comprobante c where c.empresa.id = :eid and c.fechaEmision between :d1 and :d2 "
          + "and c.estado <> 'ELIMINADO' group by c.estadoSri")
  List<EstadoSriConteo> contarPorEstadoSri(
      @Param("eid") UUID empresaId, @Param("d1") LocalDate desde, @Param("d2") LocalDate hasta);

  @Query(
      "select new ec.tusaas.efactura.dto.reporte.EstadoSriConteo(c.estadoSri, count(c)) "
          + "from Comprobante c where c.empresa.id = :eid and c.fechaEmision between :d1 and :d2 "
          + "and c.estado <> 'ELIMINADO' "
          + "and (:tipo = '' or upper(c.tipo) = upper(:tipo)) group by c.estadoSri")
  List<EstadoSriConteo> contarPorEstadoSriConTipo(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate desde,
      @Param("d2") LocalDate hasta,
      @Param("tipo") String tipoComprobante);

  @Query(
      "select new ec.tusaas.efactura.dto.reporte.ComprobanteTipoEstadoConteo(c.tipo, c.estadoSri, count(c)) "
          + "from Comprobante c where c.empresa.id = :eid and c.fechaEmision between :d1 and :d2 "
          + "and c.estado <> 'ELIMINADO' "
          + "and (:tipo = '' or upper(c.tipo) = upper(:tipo)) "
          + "group by c.tipo, c.estadoSri order by c.tipo asc, c.estadoSri asc")
  List<ec.tusaas.efactura.dto.reporte.ComprobanteTipoEstadoConteo> contarPorTipoYEstado(
      @Param("eid") UUID empresaId,
      @Param("d1") LocalDate desde,
      @Param("d2") LocalDate hasta,
      @Param("tipo") String tipoComprobante);

  @Query(
      value =
          """
          SELECT * FROM comprobante c
          WHERE c.empresa_id = :empresaId
            AND c.estado <> 'ELIMINADO'
            AND (
              c.custom_data->>'facturaModificadaId' = :facturaId
              OR c.custom_data->>'facturaOrigenId' = :facturaId
            )
          ORDER BY c.fecha_creacion DESC
          LIMIT 30
          """,
      nativeQuery = true)
  List<Comprobante> findRelacionadosConFactura(
      @Param("empresaId") UUID empresaId, @Param("facturaId") String facturaId);
}
