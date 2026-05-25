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
@Table(name = "rol")
public class Rol {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "empresa_id")
  private Empresa empresa;

  @Column(nullable = false, length = 50)
  private String codigo;

  @Column(nullable = false, length = 150)
  private String nombre;

  @Column(nullable = false)
  private boolean sistema = false;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "rol_permiso",
      joinColumns = @JoinColumn(name = "rol_id"),
      inverseJoinColumns = @JoinColumn(name = "permiso_id"))
  private Set<Permiso> permisos = new HashSet<>();

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
