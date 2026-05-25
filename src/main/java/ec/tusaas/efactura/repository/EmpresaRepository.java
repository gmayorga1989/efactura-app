package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Empresa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpresaRepository extends JpaRepository<Empresa, UUID> {

  Optional<Empresa> findByRuc(String ruc);

  Optional<Empresa> findBySlugIgnoreCase(String slug);

  boolean existsByRuc(String ruc);

  Optional<Empresa> findBySuiteCompanyId(UUID suiteCompanyId);
}
