package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.CacheConfig;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardCacheService {

  private final CacheManager cacheManager;

  public void evictEmpresa(UUID empresaId) {
    Cache cache = cacheManager.getCache(CacheConfig.DASHBOARD_CACHE);
    if (cache != null && empresaId != null) {
      cache.evictIfPresent(empresaId);
    }
  }

  public void evictAll() {
    Cache cache = cacheManager.getCache(CacheConfig.DASHBOARD_CACHE);
    if (cache != null) {
      cache.clear();
    }
  }
}
