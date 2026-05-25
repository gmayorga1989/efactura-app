package ec.tusaas.efactura.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password,
    String mfaCode,
    /** RUC de la empresa (obligatorio para usuarios de tenant). */
    String ruc) {

  public LoginRequest(String email, String password, String ruc) {
    this(email, password, null, ruc);
  }
}
