package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "plan")
public class Plan {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 50)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nombre;

  @Column(name = "precio_mensual")
  private java.math.BigDecimal precioMensual;

  @Column(name = "precio_anual")
  private java.math.BigDecimal precioAnual;

  @Column(name = "max_usuarios")
  private Integer maxUsuarios;

  @Column(name = "max_establecimientos")
  private Integer maxEstablecimientos;

  @Column(name = "incluye_api", nullable = false)
  private boolean incluyeApi = false;

  @Column(name = "incluye_scraper", nullable = false)
  private boolean incluyeScraper = false;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "caracteristicas", nullable = false, columnDefinition = "jsonb")
  private java.util.Map<String, Object> caracteristicas = new java.util.HashMap<>();

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;

  @Column(name = "fecha_modificacion")
  private Instant fechaModificacion;

  @Column(name = "usuario_modificacion", length = 100)
  private String usuarioModificacion;
}
