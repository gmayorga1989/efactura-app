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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comprobante_retencion")
public class ComprobanteRetencion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comprobante_id", nullable = false)
  private Comprobante comprobante;

  @Column(length = 4)
  private String codigo;

  @Column(name = "codigo_retencion", length = 4)
  private String codigoRetencion;

  @Column(name = "base_imponible", precision = 14, scale = 2)
  private BigDecimal baseImponible;

  @Column(precision = 8, scale = 2)
  private BigDecimal porcentaje;

  @Column(precision = 14, scale = 2)
  private BigDecimal valor;

  @Column(name = "documento_sustento_tipo", length = 2)
  private String documentoSustentoTipo;

  @Column(name = "documento_sustento_numero", length = 20)
  private String documentoSustentoNumero;

  @Column(name = "documento_sustento_fecha")
  private LocalDate documentoSustentoFecha;

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();
}
