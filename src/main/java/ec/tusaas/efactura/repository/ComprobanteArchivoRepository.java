package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ComprobanteArchivo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteArchivoRepository extends JpaRepository<ComprobanteArchivo, UUID> {

  List<ComprobanteArchivo> findByComprobante_IdOrderByFechaCreacionAsc(UUID comprobanteId);

  java.util.Optional<ComprobanteArchivo> findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(
      UUID comprobanteId, String tipo);
}
