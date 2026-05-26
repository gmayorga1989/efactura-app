package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.VendedorMeta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendedorMetaRepository extends JpaRepository<VendedorMeta, UUID> {

  List<VendedorMeta> findByVendedor_IdAndPeriodoAnioOrderByPeriodoMesAsc(UUID vendedorId, int anio);

  Optional<VendedorMeta> findByVendedor_IdAndPeriodoAnioAndPeriodoMes(UUID vendedorId, int anio, int mes);
}
