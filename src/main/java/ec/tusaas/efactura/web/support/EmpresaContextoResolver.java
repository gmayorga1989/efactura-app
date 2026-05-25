package ec.tusaas.efactura.web.support;

import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class EmpresaContextoResolver {

  /**
   * Resuelve el tenant objetivo: usuarios de empresa usan su {@code empresaId}; plataforma debe enviar {@code
   * empresaIdParam}.
   */
  public UUID resolverEmpresaId(UsuarioPrincipal principal, UUID empresaIdParam) {
    if (empresaIdParam != null) {
      if (principal.getAuthorities().stream()
          .noneMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()))) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Solo PLATFORM_ADMIN puede operar sobre otra empresa");
      }
      return empresaIdParam;
    }
    if (principal.getEmpresaId() == null) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Indique query param empresaId (operación de plataforma)");
    }
    return principal.getEmpresaId();
  }
}
