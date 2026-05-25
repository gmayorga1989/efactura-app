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
@Table(name = "certificado")
public class Certificado {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(length = 150)
  private String alias;

  @Column(name = "archivo_storage_key", nullable = false, length = 500)
  private String archivoStorageKey;

  @Column(name = "password_cifrado", nullable = false, columnDefinition = "TEXT")
  private String passwordCifrado;

  @Column(length = 300)
  private String emisor;

  @Column(length = 100)
  private String serial;

  @Column(name = "valido_desde")
  private Instant validoDesde;

  @Column(name = "valido_hasta")
  private Instant validoHasta;

  @Column(name = "activo_para_firma", nullable = false)
  private boolean activoParaFirma = false;

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
