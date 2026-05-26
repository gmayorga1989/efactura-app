package ec.tusaas.efactura.dto.cotizacion;

import java.util.UUID;

public record CotizacionAdjuntoResponse(
    UUID id,
    String tipo,
    String proveedor,
    String titulo,
    String url,
    String nombreArchivo,
    String contentType,
    Long tamanoBytes,
    int orden) {}
