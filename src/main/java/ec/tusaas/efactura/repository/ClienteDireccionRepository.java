package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ClienteDireccion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteDireccionRepository extends JpaRepository<ClienteDireccion, UUID> {

  List<ClienteDireccion> findByCliente_IdOrderByPrincipalDescTipoAsc(UUID clienteId);
}
