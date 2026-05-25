package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ComprobanteDetalle;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteDetalleRepository extends JpaRepository<ComprobanteDetalle, UUID> {

  List<ComprobanteDetalle> findByComprobante_IdOrderByLineaAsc(UUID comprobanteId);

  void deleteByComprobante_Id(UUID comprobanteId);
}
