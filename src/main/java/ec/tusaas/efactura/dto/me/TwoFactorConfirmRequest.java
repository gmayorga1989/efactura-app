package ec.tusaas.efactura.dto.me;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorConfirmRequest(
    @NotBlank @Pattern(regexp = "^\\d{6}$", message = "Codigo 2FA debe tener 6 digitos") String codigo) {}
