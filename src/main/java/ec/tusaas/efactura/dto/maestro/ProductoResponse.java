package ec.tusaas.efactura.dto.maestro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductoResponse(
    UUID id,
    UUID empresaId,
    String codigoPrincipal,
    String codigoAuxiliar,
    String descripcion,
    String tipo,
    BigDecimal precioUnitario,
    String ivaCodigo,
    String iceCodigo,
    String irbpnrCodigo,
    UUID categoriaId,
    String categoriaCodigo,
    String categoriaNombre,
    String categoriaRuta,
    String estado,
    Map<String, Object> customData,
    String imagenUrl,
    List<ProductoListaPrecioResponse> preciosListas,
    List<ProductoImpuestoAdicionalResponse> impuestosAdicionales) {}
