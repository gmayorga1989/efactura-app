package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.email.EmailPlantillaDto;
import ec.tusaas.efactura.emision.email.ComprobanteEmailRenderer;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class EmailPlantillaService {

  private final EmpresaRepository empresaRepository;
  private final EmpresaTributarioService empresaTributarioService;

  public EmailPlantillaDto desdeEmpresa(Empresa empresa) {
    if (empresa == null || empresa.getConfigExtra() == null) {
      return EmailPlantillaDto.porDefecto();
    }
    Object raw = empresa.getConfigExtra().get(EmailPlantillaDto.CONFIG_KEY);
    return EmailPlantillaDto.fromMap(raw);
  }

  @Transactional(readOnly = true)
  public EmailPlantillaDto obtener(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Empresa empresa = cargarEmpresa(empresaId);
    return desdeEmpresa(empresa);
  }

  @Transactional
  public EmailPlantillaDto guardar(UUID empresaId, EmailPlantillaDto body, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Empresa empresa = cargarEmpresa(empresaId);
    Map<String, Object> extra =
        empresa.getConfigExtra() == null ? new HashMap<>() : new HashMap<>(empresa.getConfigExtra());
    extra.put(EmailPlantillaDto.CONFIG_KEY, body.toMap());
    empresa.setConfigExtra(extra);
    empresa.setUsuarioModificacion(principal.getEmail());
    empresa.setFechaModificacion(Instant.now());
    empresaRepository.save(empresa);
    return desdeEmpresa(empresa);
  }

  @Transactional(readOnly = true)
  public String vistaPreviaHtml(UUID empresaId, EmailPlantillaDto body, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Empresa empresa = cargarEmpresa(empresaId);
    EmailPlantillaDto plantilla = body == null ? desdeEmpresa(empresa) : body;
    Comprobante demo = comprobanteDemo(empresa);
    String numero = "001-001-000000001";
    String logoUrl = resolverLogoPublico(empresa);
    return ComprobanteEmailRenderer.renderHtml(plantilla, empresa, demo, numero, logoUrl);
  }

  private static Comprobante comprobanteDemo(Empresa empresa) {
    Comprobante c = new Comprobante();
    c.setEmpresa(empresa);
    c.setFechaEmision(LocalDate.now());
    c.setRazonSocialReceptor("Cliente de ejemplo S.A.");
    c.setIdentificacionReceptor("0999999999001");
    c.setClaveAcceso("1234567890123456789012345678901234567890123456789");
    c.setValorTotal(new java.math.BigDecimal("115.00"));
    return c;
  }

  private Empresa cargarEmpresa(UUID empresaId) {
    return empresaRepository
        .findById(empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
  }

  private String resolverLogoPublico(Empresa empresa) {
    String url = empresa.getLogoUrl();
    if (url == null || url.isBlank()) {
      return null;
    }
    url = url.trim();
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }
    return null;
  }
}
