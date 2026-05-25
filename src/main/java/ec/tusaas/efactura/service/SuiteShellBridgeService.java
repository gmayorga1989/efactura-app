package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.auth.SuiteShellBridgeConsumeResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class SuiteShellBridgeService {

  private static final long TTL_SECONDS = 120;

  private record Entry(String access, String refresh, Instant expiresAt) {}

  private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

  public String put(String identityAccess, String identityRefresh) {
    String id = UUID.randomUUID().toString();
    store.put(id, new Entry(identityAccess, identityRefresh, Instant.now().plusSeconds(TTL_SECONDS)));
    return id;
  }

  /** Un solo uso: devuelve tokens y elimina la entrada. */
  public Optional<SuiteShellBridgeConsumeResponse> consume(String id) {
    if (id == null || id.isBlank()) {
      return Optional.empty();
    }
    Entry e = store.remove(id.trim());
    if (e == null) {
      return Optional.empty();
    }
    if (e.expiresAt.isBefore(Instant.now())) {
      return Optional.empty();
    }
    return Optional.of(new SuiteShellBridgeConsumeResponse(e.access(), e.refresh()));
  }
}
