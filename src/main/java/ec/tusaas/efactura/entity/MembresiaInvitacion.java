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
@Table(name = "membresia_invitacion")
public class MembresiaInvitacion {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String email;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "rol_id", nullable = false)
  private Rol rol;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  private String tokenHash;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(nullable = false, length = 20)
  private String estado = "PENDIENTE";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "invitado_por_email", length = 255)
  private String invitadoPorEmail;
}
