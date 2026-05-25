package ec.tusaas.efactura.config;

import ec.tusaas.efactura.api.ratelimit.ApiKeyRateLimiter;
import ec.tusaas.efactura.api.ratelimit.JvmApiKeyRateLimiter;
import ec.tusaas.efactura.api.ratelimit.RedisApiKeyRateLimiter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class ApiKeyRateLimiterConfiguration {

  @Bean
  @ConditionalOnProperty(prefix = "efactura.redis", name = "enabled", havingValue = "true")
  @ConditionalOnBean(StringRedisTemplate.class)
  ApiKeyRateLimiter redisApiKeyRateLimiter(StringRedisTemplate redis) {
    return new RedisApiKeyRateLimiter(redis);
  }

  @Bean
  @ConditionalOnMissingBean(ApiKeyRateLimiter.class)
  ApiKeyRateLimiter jvmApiKeyRateLimiter() {
    return new JvmApiKeyRateLimiter();
  }
}
