package ec.tusaas.efactura.dto.sridescarga;

import java.time.LocalDate;

/** Rango para sincronización manual (típicamente un mes calendario). */
public record SriSyncTriggerRequest(LocalDate fechaDesde, LocalDate fechaHasta) {}
