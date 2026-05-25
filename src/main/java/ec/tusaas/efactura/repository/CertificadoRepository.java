package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.Certificado;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificadoRepository extends JpaRepository<Certificado, UUID> {

  List<Certificado> findByEmpresa_IdOrderByFechaCreacionDesc(UUID empresaId);

  Optional<Certificado> findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(UUID empresaId);
}
