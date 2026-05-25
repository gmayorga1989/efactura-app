package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.tributario.CertificadoResponse;
import ec.tusaas.efactura.dto.tributario.EmpresaAmbienteRequest;
import ec.tusaas.efactura.dto.tributario.EstadoRequest;
import ec.tusaas.efactura.dto.tributario.EstablecimientoRequest;
import ec.tusaas.efactura.dto.tributario.EstablecimientoResponse;
import ec.tusaas.efactura.dto.tributario.PuntoEmisionRequest;
import ec.tusaas.efactura.dto.tributario.PuntoEmisionResponse;
import ec.tusaas.efactura.dto.tributario.SecuencialReservaRequest;
import ec.tusaas.efactura.dto.tributario.SecuencialReservaResponse;
import ec.tusaas.efactura.dto.tributario.SecuencialResponse;
import ec.tusaas.efactura.dto.tributario.SriEndpointsResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.CertificadoService;
import ec.tusaas.efactura.service.EmpresaTributarioService;
import ec.tusaas.efactura.service.EstablecimientoService;
import ec.tusaas.efactura.service.PuntoEmisionService;
import ec.tusaas.efactura.service.SecuencialService;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
@RequestMapping("/api/web/v1/tributario")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('PLATFORM_ADMIN') or hasAuthority('EMPRESA_ADMIN')")
public class TributarioController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final EmpresaTributarioService empresaTributarioService;
  private final EstablecimientoService establecimientoService;
  private final PuntoEmisionService puntoEmisionService;
  private final SecuencialService secuencialService;
  private final CertificadoService certificadoService;
  private final EmpresaRepository empresaRepository;

  @GetMapping("/sri-endpoints")
  public SriEndpointsResponse sriEndpoints(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return empresaTributarioService.obtenerSriEndpoints(eid, principal);
  }

  @PatchMapping("/empresa/ambiente")
  public SriEndpointsResponse actualizarAmbiente(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody EmpresaAmbienteRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return empresaTributarioService.actualizarAmbiente(eid, body, principal);
  }

  @GetMapping("/establecimientos")
  public List<EstablecimientoResponse> listarEstablecimientos(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return establecimientoService.listar(eid, principal);
  }

  @PostMapping("/establecimientos")
  public EstablecimientoResponse crearEstablecimiento(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody EstablecimientoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return establecimientoService.crear(eid, body, principal);
  }

  @GetMapping("/establecimientos/{id}")
  public EstablecimientoResponse obtenerEstablecimiento(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return establecimientoService.obtener(eid, id, principal);
  }

  @PatchMapping("/establecimientos/{id}")
  public EstablecimientoResponse actualizarEstablecimiento(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody EstablecimientoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return establecimientoService.actualizar(eid, id, body, principal);
  }

  @PatchMapping("/establecimientos/{id}/estado")
  public EstablecimientoResponse cambiarEstadoEstablecimiento(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @Valid @RequestBody EstadoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return establecimientoService.cambiarEstado(eid, id, body.estado(), principal);
  }

  @GetMapping("/establecimientos/{establecimientoId}/puntos-emision")
  public List<PuntoEmisionResponse> listarPuntos(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID establecimientoId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return puntoEmisionService.listar(eid, establecimientoId, principal);
  }

  @PostMapping("/establecimientos/{establecimientoId}/puntos-emision")
  public PuntoEmisionResponse crearPunto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID establecimientoId,
      @Valid @RequestBody PuntoEmisionRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return puntoEmisionService.crear(eid, establecimientoId, body, principal);
  }

  @GetMapping("/establecimientos/{establecimientoId}/puntos-emision/{puntoId}")
  public PuntoEmisionResponse obtenerPunto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID establecimientoId,
      @PathVariable UUID puntoId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return puntoEmisionService.obtener(eid, establecimientoId, puntoId, principal);
  }

  @PatchMapping("/establecimientos/{establecimientoId}/puntos-emision/{puntoId}")
  public PuntoEmisionResponse actualizarPunto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID establecimientoId,
      @PathVariable UUID puntoId,
      @Valid @RequestBody PuntoEmisionRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return puntoEmisionService.actualizar(eid, establecimientoId, puntoId, body, principal);
  }

  @PatchMapping("/establecimientos/{establecimientoId}/puntos-emision/{puntoId}/estado")
  public PuntoEmisionResponse cambiarEstadoPunto(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID establecimientoId,
      @PathVariable UUID puntoId,
      @Valid @RequestBody EstadoRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return puntoEmisionService.cambiarEstado(eid, establecimientoId, puntoId, body.estado(), principal);
  }

  @GetMapping("/puntos-emision/{puntoEmisionId}/secuenciales")
  public List<SecuencialResponse> listarSecuenciales(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID puntoEmisionId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return secuencialService.listar(eid, puntoEmisionId, principal);
  }

  @PostMapping("/secuenciales/reservar")
  public SecuencialReservaResponse reservarSecuencial(
      @RequestParam(required = false) UUID empresaId,
      @Valid @RequestBody SecuencialReservaRequest body,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    long v = secuencialService.reservarSiguiente(eid, body.puntoEmisionId(), body.tipoComprobante(), principal);
    return new SecuencialReservaResponse(v);
  }

  @GetMapping("/certificados")
  public List<CertificadoResponse> listarCertificados(
      @RequestParam(required = false) UUID empresaId, @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return certificadoService.listar(eid, principal);
  }

  @PostMapping(value = "/certificados", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public CertificadoResponse subirCertificado(
      @RequestParam(required = false) UUID empresaId,
      @RequestPart("archivo") MultipartFile archivo,
      @RequestParam("password") String password,
      @RequestParam(value = "alias", required = false) String alias,
      @AuthenticationPrincipal UsuarioPrincipal principal)
      throws Exception {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return certificadoService.subir(eid, archivo, password, alias, principal);
  }

  @PostMapping("/certificados/{certificadoId}/activar")
  public CertificadoResponse activarCertificado(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID certificadoId,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return certificadoService.activar(eid, certificadoId, principal);
  }

  /**
   * Vista previa de clave de acceso (49 dígitos). Los parámetros de establecimiento/punto/secuencial deben
   * corresponder a la emisión real; el aleatorio de 8 dígitos puede fijarse para pruebas.
   */
  @GetMapping("/clave-acceso-preview")
  public Map<String, String> previewClaveAcceso(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
      @RequestParam String tipoComprobante,
      @RequestParam String establecimiento,
      @RequestParam String puntoEmision,
      @RequestParam long secuencial,
      @RequestParam(required = false) String aleatorio8,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    empresaTributarioService.validarGestionEmpresa(eid, principal);
    Empresa empresa = empresaRepository.findById(eid).orElseThrow();
    String rnd = aleatorio8 == null || aleatorio8.isBlank() ? ClaveAccesoGenerator.ochoDigitosAleatorios() : aleatorio8;
    String clave =
        ClaveAccesoGenerator.generar(
            fecha,
            tipoComprobante,
            empresa.getRuc(),
            empresa.getAmbienteSri(),
            empresa.getTipoEmision(),
            establecimiento,
            puntoEmision,
            secuencial,
            rnd);
    return Map.of("claveAcceso", clave, "aleatorio8", rnd);
  }
}
