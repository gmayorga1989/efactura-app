package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "cliente")
public class Cliente {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "empresa_id", nullable = false)
  private Empresa empresa;

  @Column(name = "tipo_identificacion", nullable = false, length = 2)
  private String tipoIdentificacion;

  @Column(nullable = false, length = 20)
  private String identificacion;

  @Column(name = "razon_social", nullable = false, length = 300)
  private String razonSocial;

  @Column(name = "nombre_comercial", length = 300)
  private String nombreComercial;

  @Column(name = "tipo_tercero", nullable = false, length = 20)
  private String tipoTercero = "CLIENTE";

  @Column(length = 500)
  private String direccion;

  @Column(length = 50)
  private String telefono;

  @Column(length = 255)
  private String email;

  @Column(name = "contacto_nombre", length = 200)
  private String contactoNombre;

  @Column(name = "contacto_telefono", length = 50)
  private String contactoTelefono;

  @Column(name = "contacto_email", length = 255)
  private String contactoEmail;

  @Column(name = "obligado_contabilidad", length = 2)
  private String obligadoContabilidad;

  @Column(name = "contribuyente_especial", length = 50)
  private String contribuyenteEspecial;

  @Column(name = "regimen", length = 100)
  private String regimen;

  @Column(name = "estado_sri", length = 50)
  private String estadoSri;

  @Column(name = "actividad_economica", length = 1000)
  private String actividadEconomica;

  @Column(name = "fuente_datos", length = 30)
  private String fuenteDatos;

  @OneToMany(mappedBy = "cliente", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<ClienteDireccion> direcciones = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "custom_data", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> customData = new HashMap<>();

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();

  @Column(name = "fecha_modificacion")
  private Instant fechaModificacion;

  @Column(name = "usuario_creacion", length = 100)
  private String usuarioCreacion;

  @Column(name = "usuario_modificacion", length = 100)
  private String usuarioModificacion;
}
