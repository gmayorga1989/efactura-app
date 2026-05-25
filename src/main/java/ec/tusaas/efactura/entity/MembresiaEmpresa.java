package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "membresia_empresa")
public class MembresiaEmpresa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "identidad_id", nullable = false)
  private Identidad identidad;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "empresa_id")
  private Empresa empresa;

  @Column(nullable = false, length = 30)
  private String estado = "ACTIVO";

  @Column(name = "fecha_invitacion")
  private Instant fechaInvitacion;

  @Column(name = "fecha_aceptacion")
  private Instant fechaAceptacion;

  @Column(name = "es_ultima_empresa_usada", nullable = false)
  private boolean ultimaEmpresaUsada = false;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "membresia_rol",
      joinColumns = @JoinColumn(name = "membresia_id"),
      inverseJoinColumns = @JoinColumn(name = "rol_id"))
  private Set<Rol> roles = new HashSet<>();

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;

  @Column(name = "fecha_modificacion")
  private Instant fechaModificacion;

  @Column(name = "usuario_modificacion", length = 100)
  private String usuarioModificacion;
}
