package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Auditoria;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {

  List<Auditoria> findByEmpresa_IdAndEntidadAndEntidadIdOrderByFechaAsc(
      UUID empresaId, String entidad, UUID entidadId);
}
