package ec.tusaas.efactura.api.ratelimit;

import java.util.UUID;

/** Límite de peticiones por minuto y por API key (ventana fija de 60 s). */
public interface ApiKeyRateLimiter {

  /**
   * @return {@code true} si la petición puede continuar; {@code false} si se superó el cupo.
   */
  boolean tryConsume(UUID apiKeyId, int requestsPerMinute);
}
