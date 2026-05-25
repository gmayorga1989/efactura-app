package ec.tusaas.efactura.dto.rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RolEstadoRequest(
    @NotBlank @Pattern(regexp = "ACTIVO|INACTIVO") String estado) {}
