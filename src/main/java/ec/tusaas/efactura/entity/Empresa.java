package ec.tusaas.efactura.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "empresa")
public class Empresa {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, unique = true, length = 13)
  private String ruc;

  @Column(length = 100)
  private String slug;

  @Column(name = "razon_social", nullable = false, length = 300)
  private String razonSocial;

  @Column(name = "nombre_comercial", length = 300)
  private String nombreComercial;

  @Column(name = "obligado_contabilidad", nullable = false)
  private boolean obligadoContabilidad = false;

  @Column(name = "contribuyente_especial", length = 20)
  private String contribuyenteEspecial;

  @Column(name = "exportador_habitual", nullable = false)
  private boolean exportadorHabitual = false;

  @Column(name = "calificacion_artesanal", nullable = false)
  private boolean calificacionArtesanal = false;

  @Column(name = "codigo_artesano", length = 50)
  private String codigoArtesano;

  @Column(name = "agente_retencion", nullable = false)
  private boolean agenteRetencion = false;

  @Column(name = "ambiente_sri", nullable = false)
  private short ambienteSri = 1;

  @Column(name = "tipo_emision", nullable = false)
  private short tipoEmision = 1;

  @Column(name = "direccion_matriz", length = 500)
  private String direccionMatriz;

  @Column(name = "logo_url", length = 500)
  private String logoUrl;

  @Column(nullable = false, length = 50)
  private String timezone = "America/Guayaquil";

  /** ISO 3166-1 alpha-2; determina catálogos tributarios (impuestos adicionales, etc.). */
  @Column(name = "pais_iso", nullable = false, length = 2)
  private String paisIso = "EC";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "config_extra", nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> configExtra = new HashMap<>();

  @Column(name = "plan_codigo", nullable = false, length = 64)
  private String planCodigo = "DEMO";

  /** Límite de comprobantes por mes civil; {@code null} = sin límite. */
  @Column(name = "plan_limite_mes")
  private Integer planLimiteMes;

  /**
   * UUID del tenant en Suite Identity ({@code auth.company.id}). Debe coincidir con el claim {@code
   * company_id} del access token para el canje en {@code /auth/suite/exchange}. Si es null, se acepta
   * coincidencia por {@link #id} == {@code company_id}.
   */
  @Column(name = "suite_company_id")
  private UUID suiteCompanyId;

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
