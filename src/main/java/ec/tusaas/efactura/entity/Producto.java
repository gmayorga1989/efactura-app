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
import java.math.BigDecimal;
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
@Table(name = "producto")
public class Producto {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(name = "codigo_principal", nullable = false, length = 50)
  private String codigoPrincipal;

  @Column(name = "codigo_auxiliar", length = 50)
  private String codigoAuxiliar;

  @Column(nullable = false, length = 500)
  private String descripcion;

  @Column(length = 20)
  private String tipo;

  @Column(name = "precio_unitario", precision = 14, scale = 6)
  private BigDecimal precioUnitario;

  @Column(name = "iva_codigo", length = 4)
  private String ivaCodigo;

  @Column(name = "ice_codigo", length = 10)
  private String iceCodigo;

  @Column(name = "irbpnr_codigo", length = 10)
  private String irbpnrCodigo;

  @Column(name = "imagen_storage_key", length = 500)
  private String imagenStorageKey;

  @Column(name = "imagen_url", length = 800)
  private String imagenUrl;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "categoria_id")
  private ProductoCategoria categoria;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_data", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> customData = new HashMap<>();

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "fecha_modificacion")
  private Instant fechaModificacion;

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;

  @Column(name = "usuario_modificacion", length = 100)
  private String usuarioModificacion;
}
