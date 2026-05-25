package ec.tusaas.efactura.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DashboardComprobanteRecienteResponse(
    UUID id,
    String tipo,
    String numero,
    LocalDate fechaEmision,
    String receptor,
    String identificacion,
    BigDecimal total,
    String estadoSri) {}
