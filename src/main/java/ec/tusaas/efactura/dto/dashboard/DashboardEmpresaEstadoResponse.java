package ec.tusaas.efactura.dto.dashboard;

import java.util.UUID;

public record DashboardEmpresaEstadoResponse(
    UUID empresaId,
    String razonSocial,
    String nombreComercial,
    String ruc,
    String estado,
    short ambienteSri,
    String logoUrl,
    String planCodigo,
    Integer planLimiteMes,
    long comprobantesMes,
    boolean certificadoActivo,
    boolean configuracionBasicaCompleta) {}
