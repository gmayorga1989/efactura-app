package ec.tusaas.efactura.dto.maestro;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductoListaPrecioResponse(
    UUID listaId, String listaCodigo, String listaNombre, BigDecimal precio) {}
