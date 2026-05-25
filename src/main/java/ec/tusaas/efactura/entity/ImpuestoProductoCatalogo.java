package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "impuesto_producto_catalogo")
public class ImpuestoProductoCatalogo {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "pais_iso", nullable = false, length = 2)
  private String paisIso;

  /** Si es null, fila global del país (semilla); si no, definida por la empresa. */
  @Column(name = "empresa_id")
  private UUID empresaId;

  @Column(nullable = false, length = 40)
  private String tipo;

  @Column(nullable = false, length = 50)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(name = "porcentaje_default", precision = 10, scale = 6)
  private BigDecimal porcentajeDefault;

  @Column(nullable = false)
  private int orden;

  @Column(nullable = false)
  private boolean activo = true;
}
