package ec.tusaas.efactura.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  /**
   * Orígenes permitidos para el front (coma-separados). Por defecto se usan patrones {@code
   * http://localhost:*} para cubrir cualquier puerto de {@code ng serve}, Simple Browser de
   * Cursor, etc. En producción fija {@code CORS_ORIGINS} a orígenes concretos (HTTPS).
   */
  @Bean
  CorsConfigurationSource corsConfigurationSource(
      @Value("${efactura.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}") String allowedOrigins) {
    CorsConfiguration cfg = new CorsConfiguration();
    List<String> entries =
        Arrays.stream(allowedOrigins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    boolean anyPattern = entries.stream().anyMatch(o -> o.contains("*"));
    if (anyPattern) {
      cfg.setAllowedOriginPatterns(entries);
    } else {
      cfg.setAllowedOrigins(entries);
    }
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Authorization"));
    cfg.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", cfg);
    return source;
  }
}
