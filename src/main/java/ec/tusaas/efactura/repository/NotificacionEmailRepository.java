package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.NotificacionEmail;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificacionEmailRepository extends JpaRepository<NotificacionEmail, UUID> {

  List<NotificacionEmail> findByEmpresaIdAndTipoOrderByFechaCreacionAsc(UUID empresaId, String tipo);

  @Query(
      value =
          """
          SELECT * FROM notificacion_email n
          WHERE n.metadata->>'comprobanteId' = :comprobanteId
          ORDER BY n.fecha_creacion DESC
          LIMIT 1
          """,
      nativeQuery = true)
  Optional<NotificacionEmail> findLatestComprobanteCliente(@Param("comprobanteId") String comprobanteId);
}
