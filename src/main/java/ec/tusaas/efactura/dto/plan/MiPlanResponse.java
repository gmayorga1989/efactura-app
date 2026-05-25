package ec.tusaas.efactura.dto.plan;

import java.util.Map;

public record MiPlanResponse(
    String planCodigo,
    Integer limiteMensual,
    long emitidosPeriodo,
    String periodoDesde,
    String periodoHasta,
    boolean sinLimite,
    Map<String, Object> modulosActivos) {}
