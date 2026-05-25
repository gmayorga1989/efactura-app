package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ComprobanteGuia;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComprobanteGuiaRepository extends JpaRepository<ComprobanteGuia, UUID> {

  List<ComprobanteGuia> findByComprobante_Id(UUID comprobanteId);
}
