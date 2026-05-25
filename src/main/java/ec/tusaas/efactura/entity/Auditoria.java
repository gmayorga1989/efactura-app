package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
@Table(name = "auditoria")
public class Auditoria {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "empresa_id")
  private Empresa empresa;

  @Column(length = 150)
  private String usuario;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "api_key_id")
  private ApiKey apiKey;

  @Column(nullable = false, length = 100)
  private String accion;

  @Column(length = 100)
  private String entidad;

  @Column(name = "entidad_id")
  private UUID entidadId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "jsonb")
  private Map<String, Object> cambios = new HashMap<>();

  @Column(length = 50)
  private String ip;

  @Column(name = "user_agent", length = 500)
  private String userAgent;

  @Column(nullable = false)
  private Instant fecha = Instant.now();
}
