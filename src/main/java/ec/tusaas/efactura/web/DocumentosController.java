package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.comprobante.ComprobanteMonitorResponse;
import ec.tusaas.efactura.dto.comprobante.HistorialElectronicoEventoDto;
import ec.tusaas.efactura.dto.reporte.ComprobanteTipoEstadoConteo;
import ec.tusaas.efactura.dto.reporte.EstadoSriConteo;
import ec.tusaas.efactura.service.ComprobanteHistorialElectronicoService;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.ComprobanteMonitorService;
import ec.tusaas.efactura.service.ComprobanteReemisionSriService;
import ec.tusaas.efactura.service.DocumentoElectronicoService;
import ec.tusaas.efactura.service.GuiaRemisionElectronicaService;
import ec.tusaas.efactura.service.LiquidacionCompraElectronicaService;
import ec.tusaas.efactura.service.NotaCreditoElectronicaService;
import ec.tusaas.efactura.service.DocumentoModificadoEmisionService;
import ec.tusaas.efactura.service.RetencionElectronicaService;
import ec.tusaas.efactura.web.support.EmpresaContextoResolver;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/web/v1")
@RequiredArgsConstructor
public class DocumentosController {

  private final EmpresaContextoResolver empresaContextoResolver;
  private final DocumentoElectronicoService documentoElectronicoService;
  private final NotaCreditoElectronicaService notaCreditoElectronicaService;
  private final DocumentoModificadoEmisionService documentoModificadoEmisionService;
  private final GuiaRemisionElectronicaService guiaRemisionElectronicaService;
  private final LiquidacionCompraElectronicaService liquidacionCompraElectronicaService;
  private final RetencionElectronicaService retencionElectronicaService;
  private final ComprobanteMonitorService comprobanteMonitorService;
  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteHistorialElectronicoService comprobanteHistorialElectronicoService;
  private final ComprobanteReemisionSriService comprobanteReemisionSriService;

  @PostMapping(
      value = {
        "/ventas/notas-credito",
        "/ventas/notas-debito",
        "/ventas/guias",
        "/proveedores/retenciones",
        "/proveedores/liquidaciones"
      })
  @PreAuthorize(
      "hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('VENTAS_GESTIONAR') "
          + "or hasAuthority('PROVEEDOR_GESTIONAR')")
  public ComprobanteResponse crearDocumento(
      @RequestParam(required = false) UUID empresaId,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      @Valid @RequestBody DocumentoElectronicoRequest body,
      jakarta.servlet.http.HttpServletRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String recurso = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    return switch (recurso) {
      case "notas-credito" -> notaCreditoElectronicaService.guardarBorradorDesdeDocumento(eid, body, principal);
      case "notas-debito" -> documentoModificadoEmisionService.guardarBorradorDesdeDocumento(eid, body, principal, "NOTA_DEBITO");
      case "guias" -> guiaRemisionElectronicaService.guardarBorradorDesdeDocumento(eid, body, principal);
      case "retenciones" -> retencionElectronicaService.guardarBorradorDesdeDocumento(eid, body, principal);
      case "liquidaciones" -> liquidacionCompraElectronicaService.guardarBorradorDesdeDocumento(eid, body, principal);
      default -> documentoElectronicoService.crearBorrador(eid, recurso, body, principal);
    };
  }

  @GetMapping(
      value = {
        "/ventas/notas-credito",
        "/ventas/notas-debito",
        "/ventas/guias",
        "/proveedores/retenciones",
        "/proveedores/liquidaciones"
      })
  @PreAuthorize(
      "hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') or hasAuthority('VENTAS_GESTIONAR') "
          + "or hasAuthority('PROVEEDOR_GESTIONAR') or hasAuthority('REPORTE_VER')")
  public Page<ComprobanteMonitorResponse> listarDocumentos(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal,
      jakarta.servlet.http.HttpServletRequest request) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String recurso = request.getRequestURI().substring(request.getRequestURI().lastIndexOf('/') + 1);
    String tipo =
        switch (recurso) {
          case "notas-credito" -> "NOTA_CREDITO";
          case "notas-debito" -> "NOTA_DEBITO";
          case "guias" -> "GUIA_REMISION";
          case "retenciones" -> "RETENCION";
          case "liquidaciones" -> "LIQUIDACION_COMPRA";
          default -> recurso;
        };
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    return comprobanteMonitorService.buscar(eid, tipo, null, null, null, null, null, null, null, null, pageable);
  }

  @GetMapping("/comprobantes-electronicos")
  @PreAuthorize(
      "hasAuthority('COMPROBANTE_MONITOR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('REPORTE_VER')")
  public Page<ComprobanteMonitorResponse> monitor(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String tipoComprobante,
      @RequestParam(required = false) String estadoSri,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(required = false) String establecimiento,
      @RequestParam(required = false) String puntoEmision,
      @RequestParam(required = false) String identificacion,
      @RequestParam(required = false) String claveAcceso,
      @RequestParam(required = false) String secuencial,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    return comprobanteMonitorService.buscar(
        eid,
        tipoComprobante,
        estadoSri,
        fechaDesde,
        fechaHasta,
        establecimiento,
        puntoEmision,
        identificacion,
        claveAcceso,
        secuencial,
        pageable);
  }

  @GetMapping("/comprobantes-electronicos/resumen-estados")
  @PreAuthorize(
      "hasAuthority('COMPROBANTE_MONITOR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('REPORTE_VER')")
  public List<EstadoSriConteo> resumenEstados(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String tipoComprobante,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String tipo = tipoComprobante == null ? "" : tipoComprobante.trim();
    return comprobanteRepository.contarPorEstadoSriConTipo(eid, fechaDesde, fechaHasta, tipo);
  }

  @GetMapping("/comprobantes-electronicos/resumen-por-tipo")
  @PreAuthorize(
      "hasAuthority('COMPROBANTE_MONITOR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('REPORTE_VER')")
  public List<ComprobanteTipoEstadoConteo> resumenPorTipo(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam(required = false) String tipoComprobante,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    String tipo = tipoComprobante == null ? "" : tipoComprobante.trim();
    return comprobanteRepository.contarPorTipoYEstado(eid, fechaDesde, fechaHasta, tipo);
  }

  @PostMapping("/comprobantes-electronicos/{id}/reemitir-sri")
  @PreAuthorize(
      "hasAuthority('COMPROBANTE_EMITIR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN')")
  public ComprobanteResponse reemitirSri(
      @RequestParam(required = false) UUID empresaId,
      @PathVariable UUID id,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    return comprobanteReemisionSriService.reemitirAlSri(eid, id);
  }

  @GetMapping("/comprobantes-electronicos/historial")
  @PreAuthorize(
      "hasAuthority('COMPROBANTE_MONITOR') or hasAuthority('EMPRESA_ADMIN') or hasAuthority('PLATFORM_ADMIN') "
          + "or hasAuthority('REPORTE_VER')")
  public Page<HistorialElectronicoEventoDto> historialElectronicoEmpresa(
      @RequestParam(required = false) UUID empresaId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size,
      @AuthenticationPrincipal UsuarioPrincipal principal) {
    UUID eid = empresaContextoResolver.resolverEmpresaId(principal, empresaId);
    Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
    return comprobanteHistorialElectronicoService.historialEmpresa(eid, fechaDesde, fechaHasta, pageable);
  }
}
