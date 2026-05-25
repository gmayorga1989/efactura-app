package ec.tusaas.efactura.dto.dashboard;

import java.math.BigDecimal;

public record DashboardKpiResponse(
    String key,
    String labelKey,
    String fallbackLabel,
    BigDecimal value,
    String unit,
    String status,
    String icon) {}
