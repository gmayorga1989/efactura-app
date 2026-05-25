package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "notificacion_email")
public class NotificacionEmail {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "empresa_id")
  private UUID empresaId;

  @Column(nullable = false, length = 60)
  private String tipo;

  @Column(name = "destinatario_email", nullable = false, length = 255)
  private String destinatarioEmail;

  @Column(name = "destinatario_nombre", length = 200)
  private String destinatarioNombre;

  @Column(nullable = false, length = 300)
  private String asunto;

  @Column(nullable = false, length = 30)
  private String proveedor;

  @Column(nullable = false, length = 30)
  private String estado = "PENDIENTE";

  @Column(name = "provider_message_id", length = 200)
  private String providerMessageId;

  @Column(name = "error_mensaje", columnDefinition = "TEXT")
  private String errorMensaje;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> metadata = new HashMap<>();

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "fecha_envio")
  private Instant fechaEnvio;
}
