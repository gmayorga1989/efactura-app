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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "cotizacion_detalle")
public class CotizacionDetalle {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cotizacion_id", nullable = false)
  private Cotizacion cotizacion;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false)
  private int linea;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producto_id")
  private Producto producto;

  @Column(name = "codigo_principal", length = 50)
  private String codigoPrincipal;

  @Column(name = "codigo_auxiliar", length = 50)
  private String codigoAuxiliar;

  @Column(nullable = false, length = 500)
  private String descripcion;

  @Column(nullable = false, precision = 14, scale = 6)
  private BigDecimal cantidad;

  @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 6)
  private BigDecimal precioUnitario;

  @Column(precision = 14, scale = 2)
  private BigDecimal descuento;

  @Column(name = "iva_porcentaje", precision = 8, scale = 2)
  private BigDecimal ivaPorcentaje;

  @Column(name = "precio_total_sin_impuesto", precision = 14, scale = 2)
  private BigDecimal precioTotalSinImpuesto;

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();
}
