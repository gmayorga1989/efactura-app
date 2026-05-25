package ec.tusaas.efactura.dto.sridescarga;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SriSyncRunResponse(
    UUID id,
    UUID subscriberId,
    String tipo,
    String estado,
    LocalDate fechaDesde,
    LocalDate fechaHasta,
    int comprobantesNuevos,
    int comprobantesDuplicados,
    String mensaje,
    Instant iniciadoEn,
    Instant finalizadoEn) {}
