package ec.tusaas.efactura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;

@SpringBootApplication(
    exclude = {RedisAutoConfiguration.class, RedisRepositoriesAutoConfiguration.class})
public class EfacturaApplication {

  public static void main(String[] args) {
    SpringApplication.run(EfacturaApplication.class, args);
  }
}
