package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.MembresiaEmpresa;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MembresiaEmpresaRepository extends JpaRepository<MembresiaEmpresa, UUID> {

  boolean existsByIdentidadIdAndEmpresaId(UUID identidadId, UUID empresaId);

  boolean existsByIdentidadIdAndEmpresaIsNull(UUID identidadId);

  @EntityGraph(attributePaths = {"identidad", "roles", "roles.permisos"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m WHERE m.identidad.id = :identidadId AND m.empresa IS NULL")
  Optional<MembresiaEmpresa> findPlataformaByIdentidad(@Param("identidadId") UUID identidadId);

  @EntityGraph(attributePaths = {"identidad", "empresa", "roles", "roles.permisos"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m WHERE m.identidad.id = :identidadId "
          + "AND m.empresa.id = :empresaId")
  Optional<MembresiaEmpresa> findTenantByIdentidadAndEmpresa(
      @Param("identidadId") UUID identidadId, @Param("empresaId") UUID empresaId);

  @EntityGraph(attributePaths = {"empresa", "roles", "identidad"})
  Page<MembresiaEmpresa> findAllByEmpresa_Id(UUID empresaId, Pageable pageable);

  @EntityGraph(attributePaths = {"empresa", "roles", "roles.permisos"})
  List<MembresiaEmpresa> findAllByIdentidad_Id(UUID identidadId);

  @EntityGraph(attributePaths = {"empresa", "roles", "roles.permisos"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m WHERE m.identidad.id = :identidadId AND m.estado = :estado")
  List<MembresiaEmpresa> findAllByIdentidadAndEstado(
      @Param("identidadId") UUID identidadId, @Param("estado") String estado);

  @EntityGraph(attributePaths = {"empresa"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m WHERE m.identidad.id = :identidadId AND m.estado = 'ACTIVO'")
  List<MembresiaEmpresa> findActivasWithEmpresa(@Param("identidadId") UUID identidadId);

  Optional<MembresiaEmpresa> findByIdentidadAndEmpresa(Identidad identidad, Empresa empresa);

  Optional<MembresiaEmpresa> findByIdentidadAndEmpresaIsNull(Identidad identidad);

  /**
   * Membresía tenant activa cuyo {@link Empresa#getId()} o {@link Empresa#getSuiteCompanyId()} coincide con el
   * tenant del Identity Gateway.
   */
  @EntityGraph(attributePaths = {"identidad", "empresa", "roles", "roles.permisos"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m JOIN m.empresa e WHERE m.identidad.id = :identidadId "
          + "AND m.estado = 'ACTIVO' AND e.estado = 'ACTIVO' "
          + "AND (e.id = :suiteTenantId OR e.suiteCompanyId = :suiteTenantId)")
  Optional<MembresiaEmpresa> findActiveTenantBySuiteTenantId(
      @Param("identidadId") UUID identidadId, @Param("suiteTenantId") UUID suiteTenantId);

  @EntityGraph(attributePaths = {"identidad", "empresa", "roles", "roles.permisos"})
  @Query("SELECT m FROM MembresiaEmpresa m WHERE m.id = :id")
  Optional<MembresiaEmpresa> findWithPermisosById(@Param("id") UUID id);

  @Query("SELECT COUNT(m) FROM MembresiaEmpresa m JOIN m.roles r WHERE r.id = :rolId")
  long countByRolesId(@Param("rolId") UUID rolId);

  @EntityGraph(attributePaths = {"identidad", "empresa", "roles", "roles.permisos"})
  @Query(
      "SELECT m FROM MembresiaEmpresa m WHERE m.identidad.id = :identidadId "
          + "AND m.estado = :estado "
          + "AND (:empresaId IS NULL OR m.empresa.id = :empresaId)")
  List<MembresiaEmpresa> findAllByIdentidadEstadoAndOptionalEmpresa(
      @Param("identidadId") UUID identidadId, @Param("estado") String estado, @Param("empresaId") UUID empresaId);
}
