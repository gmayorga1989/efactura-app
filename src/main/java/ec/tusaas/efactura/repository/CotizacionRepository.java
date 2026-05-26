package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Cotizacion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CotizacionRepository extends JpaRepository<Cotizacion, UUID>, JpaSpecificationExecutor<Cotizacion> {

  Optional<Cotizacion> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  Page<Cotizacion> findByEmpresa_IdAndEstadoRegistroOrderByFechaEmisionDesc(
      UUID empresaId, String estadoRegistro, Pageable pageable);

  long countByEmpresa_IdAndNumeroStartingWith(UUID empresaId, String prefijo);

  @Query(
      """
      SELECT COALESCE(SUM(c.valorTotal), 0)
      FROM Cotizacion c
      WHERE c.empresa.id = :empresaId
        AND c.vendedor.id = :vendedorId
        AND c.estado IN ('ENVIADA', 'ACEPTADA', 'CONVERTIDA')
        AND c.fechaEmision BETWEEN :desde AND :hasta
        AND c.estadoRegistro = 'ACTIVO'
      """)
  BigDecimal sumarVentasVendedor(
      @Param("empresaId") UUID empresaId,
      @Param("vendedorId") UUID vendedorId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);

  @Query(
      """
      SELECT COUNT(c)
      FROM Cotizacion c
      WHERE c.empresa.id = :empresaId
        AND c.vendedor.id = :vendedorId
        AND c.estado = 'CONVERTIDA'
        AND c.fechaEmision BETWEEN :desde AND :hasta
        AND c.estadoRegistro = 'ACTIVO'
      """)
  long contarConversionesVendedor(
      @Param("empresaId") UUID empresaId,
      @Param("vendedorId") UUID vendedorId,
      @Param("desde") LocalDate desde,
      @Param("hasta") LocalDate hasta);
}
