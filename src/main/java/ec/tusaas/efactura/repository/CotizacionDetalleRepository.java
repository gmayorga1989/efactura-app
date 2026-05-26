package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.CotizacionDetalle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionDetalleRepository extends JpaRepository<CotizacionDetalle, UUID> {

  List<CotizacionDetalle> findByCotizacion_IdOrderByLineaAsc(UUID cotizacionId);

  void deleteByCotizacion_Id(UUID cotizacionId);
}
