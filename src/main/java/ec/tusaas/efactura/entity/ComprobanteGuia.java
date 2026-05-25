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
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "comprobante_guia")
public class ComprobanteGuia {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "comprobante_id", nullable = false)
  private Comprobante comprobante;

  @Column(name = "direccion_partida", length = 500)
  private String direccionPartida;

  @Column(name = "razon_social_transportista", length = 300)
  private String razonSocialTransportista;

  @Column(name = "tipo_id_transportista", length = 2)
  private String tipoIdTransportista;

  @Column(name = "ruc_transportista", length = 13)
  private String rucTransportista;

  @Column(length = 20)
  private String placa;

  @Column(name = "fecha_inicio_transporte")
  private LocalDate fechaInicioTransporte;

  @Column(name = "fecha_fin_transporte")
  private LocalDate fechaFinTransporte;

  @Column(name = "motivo_traslado", length = 500)
  private String motivoTraslado;

  @Column(name = "direccion_destino", length = 500)
  private String direccionDestino;

  @Column(nullable = false, length = 20)
  private String estado = "ACTIVO";

  @Column(name = "fecha_creacion", nullable = false)
  private Instant fechaCreacion = Instant.now();
}
