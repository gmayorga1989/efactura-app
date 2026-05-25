package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Cliente;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClienteRepository extends JpaRepository<Cliente, UUID>, JpaSpecificationExecutor<Cliente> {

  Page<Cliente> findByEmpresa_IdAndEstadoNotOrderByRazonSocialAsc(UUID empresaId, String estado, Pageable pageable);

  Optional<Cliente> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  Optional<Cliente> findByEmpresa_IdAndTipoIdentificacionAndIdentificacion(
      UUID empresaId, String tipoIdentificacion, String identificacion);

  @Query(
      "select count(c) from Cliente c where c.empresa.id = :empresaId and c.estado <> 'ELIMINADO' "
          + "and c.tipoTercero in :tipos")
  long countByEmpresaAndTipoTerceroIn(
      @Param("empresaId") UUID empresaId, @Param("tipos") java.util.Collection<String> tipos);
}
