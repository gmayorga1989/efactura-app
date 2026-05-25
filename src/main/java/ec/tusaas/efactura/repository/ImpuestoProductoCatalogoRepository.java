package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ImpuestoProductoCatalogo;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImpuestoProductoCatalogoRepository extends JpaRepository<ImpuestoProductoCatalogo, UUID> {

  @Query(
      "select i from ImpuestoProductoCatalogo i where i.activo = true and "
          + "((i.empresaId is null and i.paisIso = :pais) or i.empresaId = :empresaId) "
          + "order by i.orden asc, i.nombre asc")
  List<ImpuestoProductoCatalogo> listarVisiblesEmpresaPais(@Param("empresaId") UUID empresaId, @Param("pais") String pais);

  boolean existsByEmpresaIdAndTipoIgnoreCaseAndCodigoIgnoreCase(UUID empresaId, String tipo, String codigo);
}
