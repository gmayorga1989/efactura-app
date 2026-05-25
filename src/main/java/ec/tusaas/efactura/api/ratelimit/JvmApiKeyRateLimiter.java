package ec.tusaas.efactura.api.ratelimit;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limit en memoria (un solo nodo). Por defecto cuando Redis está desactivado.
 */
public class JvmApiKeyRateLimiter implements ApiKeyRateLimiter {

  private final ConcurrentHashMap<String, AtomicInteger> windows = new ConcurrentHashMap<>();

  @Override
  public boolean tryConsume(UUID apiKeyId, int requestsPerMinute) {
    int rpm = Math.max(1, requestsPerMinute);
    long minute = System.currentTimeMillis() / 60_000;
    String key = apiKeyId + ":" + minute;
    AtomicInteger counter = windows.computeIfAbsent(key, k -> new AtomicInteger(0));
    int v = counter.incrementAndGet();
    if (v > rpm) {
      counter.decrementAndGet();
      return false;
    }
    if (windows.size() > 10_000) {
      windows.clear();
    }
    return true;
  }
}
