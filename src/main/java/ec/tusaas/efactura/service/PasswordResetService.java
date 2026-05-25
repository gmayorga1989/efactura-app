package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.auth.PasswordResetConfirmRequest;
import ec.tusaas.efactura.dto.auth.PasswordResetRequest;
import ec.tusaas.efactura.entity.Identidad;
import ec.tusaas.efactura.entity.PasswordResetToken;
import ec.tusaas.efactura.repository.IdentidadRepository;
import ec.tusaas.efactura.repository.PasswordResetTokenRepository;
import ec.tusaas.efactura.security.TokenHasher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

  private static final int EXPIRACION_MINUTOS = 30;

  private final IdentidadRepository identidadRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final EmailNotificationService emailNotificationService;

  @Transactional
  public void solicitar(PasswordResetRequest req, String ip, String userAgent) {
    String email = req.email().trim().toLowerCase();
    identidadRepository
        .findByEmailIgnoreCase(email)
        .filter(i -> "ACTIVO".equalsIgnoreCase(i.getEstado()))
        .ifPresentOrElse(
            identidad -> crearTokenYEnviar(identidad, ip, userAgent),
            () -> log.info("[password-reset] solicitud para email no activo o inexistente={}", email));
  }

  @Transactional
  public void confirmar(PasswordResetConfirmRequest req) {
    String hash = TokenHasher.sha256Hex(req.token().trim());
    PasswordResetToken token =
        passwordResetTokenRepository
            .findByTokenHashAndRevokedFalseAndUsedAtIsNullAndExpiresAtAfter(hash, Instant.now())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token invalido o expirado"));
    Identidad identidad = token.getIdentidad();
    if (!"ACTIVO".equalsIgnoreCase(identidad.getEstado())) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario inactivo");
    }
    identidad.setPasswordHash(passwordEncoder.encode(req.passwordNuevo()));
    identidad.setFechaModificacion(Instant.now());
    identidad.setUsuarioModificacion(identidad.getEmail());
    identidadRepository.save(identidad);

    token.setUsedAt(Instant.now());
    passwordResetTokenRepository.save(token);
    emailNotificationService.enviarPasswordActualizado(identidad.getEmail(), identidad.getNombre());
  }

  private void crearTokenYEnviar(Identidad identidad, String ip, String userAgent) {
    String plain = TokenHasher.randomRefreshTokenPlain();
    PasswordResetToken token = new PasswordResetToken();
    token.setIdentidad(identidad);
    token.setTokenHash(TokenHasher.sha256Hex(plain));
    token.setExpiresAt(Instant.now().plus(EXPIRACION_MINUTOS, ChronoUnit.MINUTES));
    token.setRequestIp(truncate(ip, 50));
    token.setUserAgent(truncate(userAgent, 500));
    passwordResetTokenRepository.save(token);
    emailNotificationService.enviarRecuperacionPassword(
        identidad.getEmail(), identidad.getNombre(), plain, token.getExpiresAt());
  }

  private static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }
}
