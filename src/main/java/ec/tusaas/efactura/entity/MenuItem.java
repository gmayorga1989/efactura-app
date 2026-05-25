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
@Table(name = "menu_item")
public class MenuItem {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 80)
  private String codigo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "padre_id")
  private MenuItem padre;

  @Column(nullable = false)
  private int orden = 0;

  @Column(nullable = false, length = 150)
  private String etiqueta;

  @Column(name = "label_key", length = 120)
  private String labelKey;

  @Column(name = "ruta_front", length = 300)
  private String rutaFront;

  @Column(length = 80)
  private String icono;

  @Column(length = 50)
  private String modulo;

  @Column(name = "requiere_permiso_codigo", length = 100)
  private String requierePermisoCodigo;

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();
}
