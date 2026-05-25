package ec.tusaas.efactura.dto.comprobante;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ComprobanteMonitorResponse(
    UUID id,
    UUID empresaId,
    String tipo,
    String tipoCodigo,
    String establecimiento,
    String puntoEmision,
    String secuencial,
    String numeroComprobante,
    String claveAcceso,
    LocalDate fechaEmision,
    String identificacionReceptor,
    String razonSocialReceptor,
    BigDecimal valorTotal,
    String estadoSri,
    String numeroAutorizacion,
    Instant fechaAutorizacion,
    String ultimoMensajeSri,
    String emailReceptor,
    String estadoEnvioCorreo) {}
