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
@Table(name = "comprobante_archivo")
public class ComprobanteArchivo {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comprobante_id", nullable = false)
  private Comprobante comprobante;

  @Column(nullable = false, length = 30)
  private String tipo;

  @Column(name = "storage_key", nullable = false, length = 500)
  private String storageKey;

  @Column(length = 64)
  private String sha256;

  @Column(name = "tamano_bytes")
  private Long tamanoBytes;

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();
}
