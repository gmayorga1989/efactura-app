package ec.tusaas.efactura.dto.tributario;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record EmpresaAmbienteRequest(
    @NotNull @Min(1) @Max(2) Short ambienteSri, @NotNull @Min(1) @Max(9) Short tipoEmision) {}
