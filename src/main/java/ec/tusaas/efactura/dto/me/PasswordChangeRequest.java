package ec.tusaas.efactura.dto.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
    @NotBlank String passwordActual,
    @NotBlank @Size(min = 8, max = 100) String passwordNuevo) {}
