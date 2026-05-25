package ec.tusaas.efactura.dto.tributario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

public record SecuencialReservaRequest(
    @NotNull UUID puntoEmisionId,
    @NotBlank @Pattern(regexp = "\\d{2}") String tipoComprobante) {}
