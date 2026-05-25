package ec.tusaas.efactura.repository;

import ec.tusaas.efactura.entity.RefreshToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

  @Query(
      "SELECT rt FROM RefreshToken rt JOIN FETCH rt.identidad i LEFT JOIN FETCH rt.empresa "
          + "WHERE rt.tokenHash = :hash "
          + "AND rt.revoked = false AND rt.expiresAt > :now")
  Optional<RefreshToken> findValidByHash(
      @Param("hash") String hash, @Param("now") Instant now);

  @Modifying(clearAutomatically = true)
  @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.identidad.id = :identidadId")
  void revokeAllForIdentidad(@Param("identidadId") UUID identidadId);
}
