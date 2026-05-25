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
import java.time.LocalDate;
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
@Table(name = "comprobante")
public class Comprobante {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false, length = 30)
  private String tipo;

  @Column(name = "tipo_codigo", nullable = false, length = 2)
  private String tipoCodigo;

  @Column(name = "establecimiento_codigo", nullable = false, length = 3)
  private String establecimientoCodigo;

  @Column(name = "punto_emision_codigo", nullable = false, length = 3)
  private String puntoEmisionCodigo;

  @Column(nullable = false, length = 9)
  private String secuencial;

  @Column(name = "clave_acceso", nullable = false, unique = true, length = 49)
  private String claveAcceso;

  @Column(name = "fecha_emision", nullable = false)
  private LocalDate fechaEmision;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @Column(name = "razon_social_receptor", length = 300)
  private String razonSocialReceptor;

  @Column(name = "identificacion_receptor", length = 20)
  private String identificacionReceptor;

  @Column(nullable = false, length = 10)
  private String moneda = "DOLAR";

  @Column(name = "subtotal_sin_impuestos", precision = 14, scale = 2)
  private BigDecimal subtotalSinImpuestos;

  @Column(name = "subtotal_0", precision = 14, scale = 2)
  private BigDecimal subtotal0;

  @Column(name = "subtotal_12", precision = 14, scale = 2)
  private BigDecimal subtotal12;

  @Column(name = "subtotal_no_objeto", precision = 14, scale = 2)
  private BigDecimal subtotalNoObjeto;

  @Column(name = "subtotal_exento", precision = 14, scale = 2)
  private BigDecimal subtotalExento;

  @Column(name = "descuento_total", precision = 14, scale = 2)
  private BigDecimal descuentoTotal;

  @Column(name = "ice_total", precision = 14, scale = 2)
  private BigDecimal iceTotal;

  @Column(name = "iva_total", precision = 14, scale = 2)
  private BigDecimal ivaTotal;

  @Column(name = "irbpnr_total", precision = 14, scale = 2)
  private BigDecimal irbpnrTotal;

  @Column(precision = 14, scale = 2)
  private BigDecimal propina;

  @Column(name = "valor_total", precision = 14, scale = 2)
  private BigDecimal valorTotal;

  @Column(name = "estado_sri", nullable = false, length = 30)
  private String estadoSri = "GENERADO";

  @Column(name = "numero_autorizacion", length = 49)
  private String numeroAutorizacion;

  @Column(name = "fecha_autorizacion")
  private Instant fechaAutorizacion;

  @Column(name = "ambiente_sri", nullable = false)
  private short ambienteSri;

  @Column(name = "tipo_emision", nullable = false)
  private short tipoEmision;

  @Column(name = "idempotency_key", length = 100)
  private String idempotencyKey;

  @Column(length = 20)
  private String origen = "WEB";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "api_key_id")
  private ApiKey apiKey;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_data", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> customData = new HashMap<>();

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
