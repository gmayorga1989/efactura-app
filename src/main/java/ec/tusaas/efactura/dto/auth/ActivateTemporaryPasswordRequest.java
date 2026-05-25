package ec.tusaas.efactura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ActivateTemporaryPasswordRequest(
    @NotBlank @Email String email,
    @NotBlank String passwordTemporal,
    @NotBlank @Size(min = 8, max = 100) String passwordNuevo,
    UUID empresaId) {}
