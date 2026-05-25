package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClienteDireccionRequest(
    @Size(max = 30) String tipo,
    @NotBlank @Size(max = 500) String direccion,
    @Size(max = 120) String provincia,
    @Size(max = 120) String canton,
    @Size(max = 120) String parroquia,
    @Size(max = 300) String referencia,
    Boolean principal) {}
