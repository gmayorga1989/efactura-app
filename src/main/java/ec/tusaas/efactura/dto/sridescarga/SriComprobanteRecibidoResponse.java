package ec.tusaas.efactura.dto.sridescarga;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record SriComprobanteRecibidoResponse(
    UUID id,
    String claveAcceso,
    String tipoComprobante,
    String rucEmisor,
    String razonSocialEmisor,
    LocalDate fechaEmision,
    Instant fechaAutorizacion,
    BigDecimal valorTotal,
    String xmlStorageKey,
    String origen,
    boolean procesado,
    String estado,
    Instant fechaCreacion,
    Map<String, Object> metadata) {}
