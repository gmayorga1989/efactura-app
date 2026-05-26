package ec.tusaas.efactura.dto.cotizacion;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CotizacionAdjuntoRequest(
    @Size(max = 30) String proveedor,
    @Size(max = 200) String titulo,
    @NotBlank @Size(max = 2000) String url,
    Integer orden) {}
