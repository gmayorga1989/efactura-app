package ec.tusaas.efactura.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DashboardSerieDiaResponse(LocalDate fecha, BigDecimal total, long cantidad) {}
