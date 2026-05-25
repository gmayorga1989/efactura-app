package ec.tusaas.efactura.api.ratelimit;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;

@RequiredArgsConstructor
public class RedisApiKeyRateLimiter implements ApiKeyRateLimiter {

  private final StringRedisTemplate redis;

  @Override
  public boolean tryConsume(UUID apiKeyId, int requestsPerMinute) {
    int rpm = Math.max(1, requestsPerMinute);
    long minute = System.currentTimeMillis() / 60_000;
    String key = "ef:rl:apikey:" + apiKeyId + ":" + minute;
    Long c = redis.opsForValue().increment(key);
    if (c != null && c == 1) {
      redis.expire(key, Duration.ofSeconds(90));
    }
    return c != null && c <= rpm;
  }
}
