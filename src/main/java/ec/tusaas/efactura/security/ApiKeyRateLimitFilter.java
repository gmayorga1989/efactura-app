package ec.tusaas.efactura.security;

import ec.tusaas.efactura.api.ratelimit.ApiKeyRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class ApiKeyRateLimitFilter extends OncePerRequestFilter {

  private final ApiKeyRateLimiter apiKeyRateLimiter;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String path = request.getRequestURI();
    return path == null || !path.startsWith("/api/public/v1/");
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UsuarioPrincipal principal)) {
      filterChain.doFilter(request, response);
      return;
    }
    if (principal.getApiKeyId() == null || principal.getApiKeyRateLimitRpm() == null) {
      filterChain.doFilter(request, response);
      return;
    }
    int rpm = principal.getApiKeyRateLimitRpm();
    if (!apiKeyRateLimiter.tryConsume(principal.getApiKeyId(), rpm)) {
      response.setStatus(429);
      response.setContentType("application/json;charset=UTF-8");
      response.getWriter()
          .write(
              "{\"title\":\"Too Many Requests\",\"detail\":\"Límite de peticiones por minuto de la API key ("
                  + rpm
                  + " rpm)\"}");
      return;
    }
    filterChain.doFilter(request, response);
  }
}
