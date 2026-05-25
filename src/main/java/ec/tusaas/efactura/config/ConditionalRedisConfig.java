package ec.tusaas.efactura.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
@ConditionalOnProperty(prefix = "efactura.redis", name = "enabled", havingValue = "true")
public class ConditionalRedisConfig {

  @Bean
  LettuceConnectionFactory redisConnectionFactory(
      @Value("${spring.data.redis.host:localhost}") String host,
      @Value("${spring.data.redis.port:6379}") int port) {
    RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
    return new LettuceConnectionFactory(cfg);
  }

  @Bean
  StringRedisTemplate stringRedisTemplate(LettuceConnectionFactory redisConnectionFactory) {
    StringRedisTemplate t = new StringRedisTemplate();
    t.setConnectionFactory(redisConnectionFactory);
    return t;
  }
}
