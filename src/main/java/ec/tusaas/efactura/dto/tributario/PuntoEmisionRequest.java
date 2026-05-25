package ec.tusaas.efactura.dto.tributario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PuntoEmisionRequest(
    @NotBlank @Size(min = 3, max = 3) @Pattern(regexp = "\\d{3}") String codigo,
    @Size(max = 200) String nombre) {}
