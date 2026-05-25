package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.PasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

  Optional<PasswordResetToken> findByTokenHashAndRevokedFalseAndUsedAtIsNullAndExpiresAtAfter(
      String tokenHash, Instant now);
}
