package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.CotizacionAdjunto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionAdjuntoRepository extends JpaRepository<CotizacionAdjunto, UUID> {

  List<CotizacionAdjunto> findByCotizacion_IdAndEstadoOrderByOrdenAsc(UUID cotizacionId, String estado);

  void deleteByCotizacion_Id(UUID cotizacionId);
}
