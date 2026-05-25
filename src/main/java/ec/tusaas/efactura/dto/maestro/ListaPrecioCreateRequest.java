package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ListaPrecioCreateRequest(
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    Boolean esDefault) {}
