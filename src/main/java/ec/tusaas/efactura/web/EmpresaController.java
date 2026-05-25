package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.empresa.EmpresaCreateRequest;
import ec.tusaas.efactura.dto.empresa.EmpresaLogoResponse;
import ec.tusaas.efactura.dto.empresa.EmpresaResponse;
import ec.tusaas.efactura.dto.empresa.EmpresaUpdateRequest;
import ec.tusaas.efactura.dto.email.EmailPlantillaDto;
import ec.tusaas.efactura.dto.ride.RidePlantillaDto;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.EmpresaLogoService;
import ec.tusaas.efactura.service.EmpresaService;
import ec.tusaas.efactura.service.EmailPlantillaService;
import ec.tusaas.efactura.service.RidePlantillaService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/web/v1/empresas")
@RequiredArgsConstructor
public class EmpresaController {

  private final EmpresaService empresaService;
  private final EmpresaLogoService empresaLogoService;
  private final RidePlantillaService ridePlantillaService;
  private final EmailPlantillaService emailPlantillaService;

  @GetMapping("/{id}")
  public EmpresaResponse obtener(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    return empresaService.obtener(id, principal);
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
  public EmpresaResponse crear(@Valid @RequestBody EmpresaCreateRequest request) {
    return empresaService.crear(request);
  }

  @PatchMapping("/{id}")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public EmpresaResponse actualizar(
      @PathVariable UUID id,
      @Valid @RequestBody EmpresaUpdateRequest request,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return empresaService.actualizar(id, request, principal);
  }

  @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public EmpresaLogoResponse subirLogo(
      @PathVariable UUID id,
      @RequestPart("archivo") MultipartFile archivo,
      @AuthenticationPrincipal UsuarioPrincipal principal)
      throws Exception {
    return empresaLogoService.subir(id, archivo, principal);
  }

  @GetMapping("/{id}/ride-plantilla")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public RidePlantillaDto obtenerRidePlantilla(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "FACTURA") String tipo,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return ridePlantillaService.obtener(id, tipo, principal);
  }

  @org.springframework.web.bind.annotation.PutMapping("/{id}/ride-plantilla")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public RidePlantillaDto guardarRidePlantilla(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "FACTURA") String tipo,
      @Valid @RequestBody RidePlantillaDto body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return ridePlantillaService.guardar(id, tipo, body, principal);
  }

  @PostMapping(value = "/{id}/ride-plantilla/vista-previa", produces = MediaType.APPLICATION_PDF_VALUE)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ResponseEntity<byte[]> vistaPreviaRidePlantilla(
      @PathVariable UUID id,
      @RequestParam(defaultValue = "FACTURA") String tipo,
      @RequestBody(required = false) RidePlantillaDto body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    RidePlantillaDto plantilla =
        body == null ? ridePlantillaService.obtener(id, tipo, principal) : body;
    byte[] pdf = ridePlantillaService.vistaPrevia(id, tipo, plantilla, principal);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"ride-vista-previa.pdf\"")
        .contentType(MediaType.APPLICATION_PDF)
        .body(pdf);
  }

  @GetMapping("/{id}/email-plantilla")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public EmailPlantillaDto obtenerEmailPlantilla(
      @PathVariable UUID id, @AuthenticationPrincipal UsuarioPrincipal principal) {
    return emailPlantillaService.obtener(id, principal);
  }

  @org.springframework.web.bind.annotation.PutMapping("/{id}/email-plantilla")
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public EmailPlantillaDto guardarEmailPlantilla(
      @PathVariable UUID id,
      @Valid @RequestBody EmailPlantillaDto body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    return emailPlantillaService.guardar(id, body, principal);
  }

  @PostMapping(value = "/{id}/email-plantilla/vista-previa", produces = MediaType.TEXT_HTML_VALUE)
  @PreAuthorize("hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ResponseEntity<String> vistaPreviaEmailPlantilla(
      @PathVariable UUID id,
      @RequestBody(required = false) EmailPlantillaDto body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    String html = emailPlantillaService.vistaPreviaHtml(id, body, principal);
    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
  }
}
