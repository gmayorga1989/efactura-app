package ec.tusaas.efactura.dto.vendedor;

import java.math.BigDecimal;
import java.util.UUID;

public record VendedorMetaResponse(
    UUID id,
    int periodoAnio,
    int periodoMes,
    BigDecimal metaMonto,
    Integer metaCantidad,
    String notas) {}
