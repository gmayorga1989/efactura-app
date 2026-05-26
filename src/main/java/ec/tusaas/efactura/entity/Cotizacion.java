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
@Table(name = "cotizacion")
public class Cotizacion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false, length = 30)
  private String numero;

  @Column(name = "fecha_emision", nullable = false)
  private LocalDate fechaEmision;

  @Column(name = "validez_dias", nullable = false)
  private int validezDias = 15;

  @Column(nullable = false, length = 30)
  private String estado = "BORRADOR";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cliente_id")
  private Cliente cliente;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "vendedor_id")
  private Vendedor vendedor;

  @Column(name = "tipo_identificacion_receptor", length = 2)
  private String tipoIdentificacionReceptor;

  @Column(name = "identificacion_receptor", length = 20)
  private String identificacionReceptor;

  @Column(name = "razon_social_receptor", length = 300)
  private String razonSocialReceptor;

  @Column(name = "email_receptor", length = 255)
  private String emailReceptor;

  @Column(nullable = false, length = 10)
  private String moneda = "DOLAR";

  @Column(name = "subtotal_sin_impuestos", precision = 14, scale = 2)
  private BigDecimal subtotalSinImpuestos;

  @Column(name = "descuento_total", precision = 14, scale = 2)
  private BigDecimal descuentoTotal;

  @Column(name = "iva_total", precision = 14, scale = 2)
  private BigDecimal ivaTotal;

  @Column(name = "valor_total", precision = 14, scale = 2)
  private BigDecimal valorTotal;

  @Column(name = "introduccion_html", columnDefinition = "text")
  private String introduccionHtml;

  @Column(name = "condiciones_html", columnDefinition = "text")
  private String condicionesHtml;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "plantilla_json", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> plantillaJson = new HashMap<>();

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comprobante_id")
  private Comprobante comprobante;

  @Column(name = "fecha_envio")
  private Instant fechaEnvio;

  @Column(name = "fecha_conversion")
  private Instant fechaConversion;

  @Column(name = "estado_registro", nullable = false, length = 20)
  private String estadoRegistro = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;

  @Column(name = "fecha_modificacion")
  private Instant fechaModificacion;

  @Column(name = "usuario_modificacion", length = 100)
  private String usuarioModificacion;
}
