package ec.tusaas.efactura.dto.reporte;

import java.time.LocalDate;
import java.util.UUID;

public record ReporteGuiaResponse(
    UUID comprobanteId,
    String numero,
    String claveAcceso,
    LocalDate fechaEmision,
    String estadoSri,
    String razonSocialDestinatario,
    String identificacionDestinatario,
    String direccionPartida,
    String direccionDestino,
    String motivoTraslado,
    String razonSocialTransportista,
    String rucTransportista,
    String placa,
    LocalDate fechaInicioTransporte,
    LocalDate fechaFinTransporte) {}
