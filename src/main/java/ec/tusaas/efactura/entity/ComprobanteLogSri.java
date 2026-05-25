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
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comprobante_log_sri")
public class ComprobanteLogSri {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "comprobante_id")
  private Comprobante comprobante;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(nullable = false, length = 30)
  private String operacion;

  @Column(columnDefinition = "TEXT")
  private String request;

  @Column(columnDefinition = "TEXT")
  private String response;

  @Column(name = "http_status")
  private Integer httpStatus;

  @Column(name = "duracion_ms")
  private Integer duracionMs;

  @Column(name = "error_codigo", length = 20)
  private String errorCodigo;

  @Column(name = "error_mensaje", columnDefinition = "TEXT")
  private String errorMensaje;

  @Column(nullable = false)
  private Instant fecha = Instant.now();
}
