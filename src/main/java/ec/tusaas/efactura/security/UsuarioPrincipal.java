package ec.tusaas.efactura.security;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class UsuarioPrincipal implements UserDetails {

  /** Identidad de login (coincide con el {@code sub} del JWT de acceso). */
  private final UUID id;
  /** Empresa del usuario; null si es administrador de plataforma. */
  private final UUID empresaId;

  /** No null si la sesión proviene de una API key (mismo valor que {@link #id} en ese caso). */
  private final UUID apiKeyId;

  /** Cupo rpm de la API key; null si no aplica. */
  private final Integer apiKeyRateLimitRpm;

  private final String email;
  private final String passwordHash;
  private final boolean activo;
  private final Collection<? extends GrantedAuthority> authorities;

  public UsuarioPrincipal(
      UUID id,
      UUID empresaId,
      String email,
      String passwordHash,
      boolean activo,
      Collection<String> authorityCodes,
      UUID apiKeyId,
      Integer apiKeyRateLimitRpm) {
    this.id = id;
    this.empresaId = empresaId;
    this.apiKeyId = apiKeyId;
    this.apiKeyRateLimitRpm = apiKeyRateLimitRpm;
    this.email = email;
    this.passwordHash = passwordHash;
    this.activo = activo;
    this.authorities =
        authorityCodes == null
            ? Collections.emptyList()
            : authorityCodes.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toUnmodifiableSet());
  }

  public static UsuarioPrincipal authenticated(
      UUID id, UUID empresaId, String email, Collection<String> authorityCodes) {
    return new UsuarioPrincipal(id, empresaId, email, "", true, authorityCodes, null, null);
  }

  public static UsuarioPrincipal authenticatedWithApiKey(
      UUID apiKeyId,
      UUID empresaId,
      String label,
      Collection<String> authorityCodes,
      int rateLimitRpm) {
    return new UsuarioPrincipal(
        apiKeyId,
        empresaId,
        label,
        "",
        true,
        authorityCodes,
        apiKeyId,
        Math.max(1, rateLimitRpm));
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  @Override
  public String getPassword() {
    return passwordHash;
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return true;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return activo;
  }
}
