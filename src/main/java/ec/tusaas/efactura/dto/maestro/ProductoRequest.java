package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ProductoRequest(
    @NotBlank @Size(max = 50) String codigoPrincipal,
    @Size(max = 50) String codigoAuxiliar,
    @NotBlank @Size(max = 300) String descripcion,
    @Size(max = 20) String tipo,
    BigDecimal precioUnitario,
    @Size(max = 4) String ivaCodigo,
    @Size(max = 10) String iceCodigo,
    @Size(max = 10) String irbpnrCodigo,
    UUID categoriaId,
    Map<String, Object> customData,
    List<@Valid ProductoListaPrecioRequest> preciosListas,
    List<@Valid ProductoImpuestoCatalogoRequest> impuestosCatalogo,
    List<@Valid ProductoImpuestoManualRequest> impuestosManuales) {

  public Map<String, Object> safeCustomData() {
    return customData == null ? new HashMap<>() : customData;
  }

  public List<ProductoListaPrecioRequest> safePreciosListas() {
    return preciosListas == null ? List.of() : preciosListas;
  }

  public List<ProductoImpuestoCatalogoRequest> safeImpuestosCatalogo() {
    return impuestosCatalogo == null ? List.of() : impuestosCatalogo;
  }

  public List<ProductoImpuestoManualRequest> safeImpuestosManuales() {
    return impuestosManuales == null ? List.of() : impuestosManuales;
  }
}
