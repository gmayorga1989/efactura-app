package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Rol;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RolRepository extends JpaRepository<Rol, UUID> {

  @EntityGraph(attributePaths = "permisos")
  @Query("SELECT r FROM Rol r WHERE r.empresa.id = :eid ORDER BY r.nombre ASC")
  List<Rol> findAllByEmpresa_IdWithPermisosOrderByNombre(@Param("eid") UUID empresaId);

  @EntityGraph(attributePaths = "permisos")
  @Query(
      "SELECT r FROM Rol r WHERE r.empresa.id = :eid "
          + "AND (:estado IS NULL OR r.estado = :estado) "
          + "ORDER BY r.nombre ASC")
  List<Rol> findAllByEmpresaIdAndOptionalEstadoWithPermisos(
      @Param("eid") UUID empresaId, @Param("estado") String estado);

  Optional<Rol> findByIdAndEmpresa_Id(UUID id, UUID empresaId);

  Optional<Rol> findByEmpresaIsNullAndCodigo(String codigo);

  Optional<Rol> findByEmpresaAndCodigo(Empresa empresa, String codigo);

  List<Rol> findAllByEmpresaAndCodigoIn(Empresa empresa, List<String> codigos);

  @Query(
      "SELECT DISTINCT r FROM Rol r LEFT JOIN FETCH r.permisos WHERE r.id = :id")
  Optional<Rol> findByIdWithPermisos(@Param("id") UUID id);

  @Query(
      "SELECT DISTINCT r FROM Rol r LEFT JOIN FETCH r.permisos WHERE r.id = :id AND r.empresa.id = :empresaId")
  Optional<Rol> findByIdAndEmpresaIdWithPermisos(@Param("id") UUID id, @Param("empresaId") UUID empresaId);
}
