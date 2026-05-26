package ec.tusaas.efactura.dto.cotizacion;

import java.util.UUID;

public record CotizacionAdjuntoResponse(
    UUID id, String proveedor, String titulo, String url, int orden) {}
