package ec.tusaas.efactura.dto.tributario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record EstadoRequest(
    @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO") String estado) {}
