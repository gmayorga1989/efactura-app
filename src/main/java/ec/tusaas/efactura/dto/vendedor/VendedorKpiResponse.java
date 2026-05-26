package ec.tusaas.efactura.dto.vendedor;

import java.math.BigDecimal;
import java.util.UUID;

public record VendedorKpiResponse(
    UUID vendedorId,
    String nombreCompleto,
    int periodoAnio,
    int periodoMes,
    BigDecimal metaMonto,
    BigDecimal ventasMonto,
    long cotizacionesConvertidas,
    BigDecimal porcentajeAvance) {}
