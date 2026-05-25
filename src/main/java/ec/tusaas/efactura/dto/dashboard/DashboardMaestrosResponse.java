package ec.tusaas.efactura.dto.dashboard;

public record DashboardMaestrosResponse(
    long clientes,
    long proveedores,
    long productos,
    long servicios,
    long establecimientos,
    long puntosEmision) {}
