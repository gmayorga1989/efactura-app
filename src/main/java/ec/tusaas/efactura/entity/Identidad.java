package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "identidad")
public class Identidad {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 255)
  private String email;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(nullable = false, length = 200)
  private String nombre;

  @Column(length = 30)
  private String genero;

  @Column(name = "fecha_nacimiento")
  private java.time.LocalDate fechaNacimiento;

  @Column(length = 2)
  private String pais;

  @Column(length = 120)
  private String provincia;

  @Column(length = 120)
  private String canton;

  @Column(length = 120)
  private String ciudad;

  @Column(length = 120)
  private String parroquia;

  @Column(name = "idioma", nullable = false, length = 10)
  private String idioma = "es";

  @Column(name = "moneda", nullable = false, length = 3)
  private String moneda = "USD";

  @Column(name = "zona_horaria", nullable = false, length = 80)
  private String zonaHoraria = "America/Guayaquil";

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "avatar_storage_key", length = 500)
  private String avatarStorageKey;

  @Column(name = "ultimo_ping")
  private Instant ultimoPing;

  @Column(name = "mfa_secret", length = 100)
  private String mfaSecret;

  @Column(name = "mfa_habilitado", nullable = false)
  private boolean mfaHabilitado = false;

  @Column(name = "ultimo_login")
  private Instant ultimoLogin;

  @Column(name = "intentos_fallidos", nullable = false)
  private int intentosFallidos = 0;

  @Column(name = "bloqueado_hasta")
  private Instant bloqueadoHasta;

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

  @OneToMany(mappedBy = "identidad")
  private List<MembresiaEmpresa> membresias = new ArrayList<>();
}
