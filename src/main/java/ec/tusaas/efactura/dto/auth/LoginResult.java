package ec.tusaas.efactura.dto.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LoginResult(
    String loginStep,
    TokenResponse tokens,
    String sessionTicket,
    List<EmpresaLoginOptionDto> empresas,
    Boolean mfaRequired) {

  public static LoginResult complete(TokenResponse tokens) {
    return new LoginResult("COMPLETE", tokens, null, null, null);
  }

  public static LoginResult selectEmpresa(String sessionTicket, List<EmpresaLoginOptionDto> empresas) {
    return new LoginResult("SELECT_EMPRESA", null, sessionTicket, empresas, null);
  }

  public static LoginResult requireMfa() {
    return new LoginResult("MFA_REQUIRED", null, null, null, true);
  }
}
