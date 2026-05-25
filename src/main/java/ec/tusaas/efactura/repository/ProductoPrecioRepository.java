package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.ProductoPrecio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductoPrecioRepository extends JpaRepository<ProductoPrecio, UUID> {

  @Query(
      "select pp from ProductoPrecio pp join fetch pp.listaPrecio lp where pp.producto.id = :productoId order by lp.codigo")
  List<ProductoPrecio> findByProductoIdWithLista(@Param("productoId") UUID productoId);

  Optional<ProductoPrecio> findByProducto_IdAndListaPrecio_Id(UUID productoId, UUID listaPrecioId);

  @Modifying
  @Query("delete from ProductoPrecio pp where pp.producto.id = :productoId")
  void deleteByProducto_Id(@Param("productoId") UUID productoId);
}
