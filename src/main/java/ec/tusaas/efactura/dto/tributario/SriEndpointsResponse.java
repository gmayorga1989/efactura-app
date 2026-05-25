package ec.tusaas.efactura.dto.tributario;

public record SriEndpointsResponse(
    int ambienteSri,
    int tipoEmision,
    String recepcionUrl,
    String autorizacionUrl,
    int recepcionTimeoutMs,
    int autorizacionTimeoutMs) {}
