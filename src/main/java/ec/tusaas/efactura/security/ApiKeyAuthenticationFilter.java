package ec.tusaas.efactura.security;

import ec.tusaas.efactura.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  public static final String HEADER = "X-Api-Key";

  private final ApiKeyService apiKeyService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    if (path == null) {
      return true;
    }
    if (!path.startsWith("/api/public/v1/")) {
      return true;
    }
    // Rutas de solo navegador (alineadas con SecurityConfig permitAll): no usan X-Api-Key.
    if ("/api/public/v1/auth/suite-identity".equals(path)
        || "/api/public/v1/auth/suite/bootstrap".equals(path)
        || "/api/public/v1/auth/suite-shell-bridge".equals(path)
        || path.startsWith("/api/public/v1/auth/suite-shell-bridge/")) {
      return true;
    }
    if (path.startsWith("/api/public/v1/storage/logos/")
        || path.startsWith("/api/public/v1/storage/avatars/")) {
      return true;
    }
    return false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    var authOpt = apiKeyService.autenticar(header);
    if (authOpt.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter().write("{\"title\":\"Unauthorized\",\"detail\":\"Se requiere cabecera X-Api-Key válida\"}");
      return;
    }
    UsuarioPrincipal principal = authOpt.get();
    var token =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    token.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(token);
    filterChain.doFilter(request, response);
  }
}
