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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "api_key")
public class ApiKey {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(length = 150)
  private String nombre;

  @Column(nullable = false, unique = true, length = 32)
  private String prefix;

  @Column(name = "key_hash", nullable = false, length = 255)
  private String keyHash;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<String> scopes = new ArrayList<>();

  @Column(name = "rate_limit_rpm", nullable = false)
  private int rateLimitRpm = 60;

  @Column(name = "fecha_expiracion")
  private Instant fechaExpiracion;

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVA";

  @Column(name = "ultimo_uso")
  private Instant ultimoUso;

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;
}
