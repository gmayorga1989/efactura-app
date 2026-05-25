package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.tenant.TenantDatasourceConfigRequest;
import ec.tusaas.efactura.dto.tenant.TenantDatasourceConfigResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.TenantDatasourceConfig;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.TenantDatasourceConfigRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class TenantDatasourceConfigService {

  private final TenantDatasourceConfigRepository repository;
  private final EmpresaRepository empresaRepository;

  @Transactional(readOnly = true)
  public TenantDatasourceConfigResponse obtener(UUID empresaId) {
    return toResponse(obtenerOError(empresaId));
  }

  @Transactional(readOnly = true)
  public List<TenantDatasourceConfigResponse> listarPorDatasource(String datasourceKey) {
    return repository.findByDatasourceKeyAndEstadoOrderByEmpresa_RazonSocialAsc(datasourceKey, "ACTIVO").stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public TenantDatasourceConfigResponse asegurarShared(Empresa empresa, String usuario) {
    return toResponse(
        repository
            .findByEmpresa_Id(empresa.getId())
            .orElseGet(() -> repository.save(nuevaShared(empresa, usuario))));
  }

  @Transactional
  public TenantDatasourceConfigResponse actualizar(
      UUID empresaId, TenantDatasourceConfigRequest request, UsuarioPrincipal principal) {
    TenantDatasourceConfig config =
        repository
            .findByEmpresa_Id(empresaId)
            .orElseGet(
                () -> {
                  Empresa empresa =
                      empresaRepository
                          .findById(empresaId)
                          .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
                  return nuevaShared(empresa, principal.getEmail());
                });

    validar(request);
    config.setModoTenant(request.modoTenant());
    config.setDatasourceKey(request.datasourceKey().trim());
    config.setEstado(request.estado() == null || request.estado().isBlank() ? "ACTIVO" : request.estado().trim());
    config.setFechaModificacion(Instant.now());
    config.setUsuarioModificacion(principal.getEmail());
    return toResponse(repository.save(config));
  }

  public TenantDatasourceConfig obtenerOError(UUID empresaId) {
    return repository
        .findByEmpresa_Id(empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuracion tenant no encontrada"));
  }

  private TenantDatasourceConfig nuevaShared(Empresa empresa, String usuario) {
    TenantDatasourceConfig config = new TenantDatasourceConfig();
    config.setEmpresa(empresa);
    config.setModoTenant(TenantDatasourceConfig.MODO_SHARED);
    config.setDatasourceKey(TenantDatasourceConfig.DEFAULT_DATASOURCE_KEY);
    config.setEstado("ACTIVO");
    config.setUsuarioCreacion(usuario);
    return config;
  }

  private static void validar(TenantDatasourceConfigRequest request) {
    String datasourceKey = request.datasourceKey().trim();
    if (TenantDatasourceConfig.MODO_SHARED.equals(request.modoTenant())
        && !TenantDatasourceConfig.DEFAULT_DATASOURCE_KEY.equals(datasourceKey)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SHARED debe usar datasourceKey shared-main");
    }
    if (TenantDatasourceConfig.MODO_DEDICATED.equals(request.modoTenant())
        && TenantDatasourceConfig.DEFAULT_DATASOURCE_KEY.equals(datasourceKey)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DEDICATED requiere un datasourceKey dedicado");
    }
  }

  private TenantDatasourceConfigResponse toResponse(TenantDatasourceConfig config) {
    Empresa empresa = config.getEmpresa();
    return new TenantDatasourceConfigResponse(
        config.getId(),
        empresa.getId(),
        empresa.getRuc(),
        empresa.getSlug(),
        empresa.getRazonSocial(),
        config.getModoTenant(),
        config.getDatasourceKey(),
        config.getEstado(),
        config.getFechaCreacion(),
        config.getFechaModificacion());
  }
}
