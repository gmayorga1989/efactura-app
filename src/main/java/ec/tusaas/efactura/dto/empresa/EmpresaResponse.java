package ec.tusaas.efactura.dto.empresa;

import java.util.Map;
import java.util.UUID;

public record EmpresaResponse(
    UUID id,
    String ruc,
    String slug,
    String razonSocial,
    String nombreComercial,
    boolean obligadoContabilidad,
    String contribuyenteEspecial,
    boolean exportadorHabitual,
    boolean calificacionArtesanal,
    String codigoArtesano,
    boolean agenteRetencion,
    short ambienteSri,
    short tipoEmision,
    String direccionMatriz,
    String logoUrl,
    String timezone,
    String paisIso,
    String estado,
    Map<String, Object> configExtra) {}
