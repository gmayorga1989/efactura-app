package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.SriProperties;
import ec.tusaas.efactura.dto.tributario.EmpresaAmbienteRequest;
import ec.tusaas.efactura.dto.tributario.SriEndpointsResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmpresaTributarioService {

  private final EmpresaRepository empresaRepository;
  private final SriProperties sriProperties;

  @Transactional(readOnly = true)
  public SriEndpointsResponse obtenerSriEndpoints(UUID empresaId, UsuarioPrincipal principal) {
    validarGestionEmpresa(empresaId, principal);
    Empresa e =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    boolean pruebas = e.getAmbienteSri() == 1;
    return new SriEndpointsResponse(
        e.getAmbienteSri(),
        e.getTipoEmision(),
        pruebas ? sriProperties.getRecepcionUrlPruebas() : sriProperties.getRecepcionUrlProduccion(),
        pruebas ? sriProperties.getAutorizacionUrlPruebas() : sriProperties.getAutorizacionUrlProduccion(),
        sriProperties.getRecepcionTimeoutMs(),
        sriProperties.getAutorizacionTimeoutMs());
  }

  @Transactional
  public SriEndpointsResponse actualizarAmbiente(
      UUID empresaId, EmpresaAmbienteRequest req, UsuarioPrincipal principal) {
    validarGestionEmpresa(empresaId, principal);
    Empresa e =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    e.setAmbienteSri(req.ambienteSri());
    e.setTipoEmision(req.tipoEmision());
    empresaRepository.save(e);
    boolean pruebas = e.getAmbienteSri() == 1;
    return new SriEndpointsResponse(
        e.getAmbienteSri(),
        e.getTipoEmision(),
        pruebas ? sriProperties.getRecepcionUrlPruebas() : sriProperties.getRecepcionUrlProduccion(),
        pruebas ? sriProperties.getAutorizacionUrlPruebas() : sriProperties.getAutorizacionUrlProduccion(),
        sriProperties.getRecepcionTimeoutMs(),
        sriProperties.getAutorizacionTimeoutMs());
  }

  public void validarGestionEmpresa(UUID empresaId, UsuarioPrincipal principal) {
    assertPuedeGestionarEmpresa(empresaId, principal);
  }

  private void assertPuedeGestionarEmpresa(UUID empresaId, UsuarioPrincipal principal) {
    if (principal.getAuthorities().stream().anyMatch(a -> "PLATFORM_ADMIN".equals(a.getAuthority()))) {
      return;
    }
    if (principal.getEmpresaId() != null
        && principal.getEmpresaId().equals(empresaId)
        && principal.getAuthorities().stream()
            .anyMatch(
                a ->
                    "EMPRESA_ADMIN".equals(a.getAuthority())
                        || "FACTURA_EMITIR".equals(a.getAuthority()))) {
      return;
    }
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN, "Se requiere permiso de la empresa o PLATFORM_ADMIN");
  }
}
