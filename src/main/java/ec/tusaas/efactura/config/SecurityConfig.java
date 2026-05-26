package ec.tusaas.efactura.config;

import ec.tusaas.efactura.security.ApiKeyAuthenticationFilter;
import ec.tusaas.efactura.security.ApiKeyRateLimitFilter;
import ec.tusaas.efactura.security.JwtAuthenticationFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
  private final ApiKeyRateLimitFilter apiKeyRateLimitFilter;

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.dispatcherTypeMatchers(DispatcherType.ASYNC)
                    .permitAll()
                    .requestMatchers(
                        "/actuator/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/api/public/version",
                        "/api/public/v1/auth/suite-identity",
                        "/api/public/v1/auth/suite/bootstrap",
                        "/api/public/v1/auth/suite-shell-bridge",
                        "/api/public/v1/auth/suite-shell-bridge/**",
                        "/api/public/v1/storage/logos/**",
                        "/api/public/v1/storage/avatars/**",
                        "/error")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/api/web/v1/auth/login",
                        "/api/web/v1/auth/refresh",
                        "/api/web/v1/auth/select-empresa",
                        "/api/web/v1/auth/accept-invite",
                        "/api/web/v1/auth/activate-temporary-password",
                        "/api/web/v1/auth/password-reset/request",
                        "/api/web/v1/auth/password-reset/confirm",
                        "/api/web/v1/auth/suite/exchange")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/web/v1/integraciones/cloud/google/callback",
                        "/api/web/v1/integraciones/cloud/microsoft/callback")
                    .permitAll()
                    .requestMatchers("/api/web/v1/**")
                    .authenticated()
                    .requestMatchers("/api/public/v1/**")
                    .authenticated()
                    .anyRequest()
                    .denyAll())
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(apiKeyRateLimitFilter, JwtAuthenticationFilter.class)
        .addFilterBefore(apiKeyAuthenticationFilter, ApiKeyRateLimitFilter.class);
    return http.build();
  }
}
