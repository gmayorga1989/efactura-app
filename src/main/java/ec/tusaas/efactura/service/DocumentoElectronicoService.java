package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.documento.DocumentoElectronicoRequest;
import ec.tusaas.efactura.dto.emision.ComprobanteResponse;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.sri.ClaveAccesoGenerator;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import ec.tusaas.efactura.util.ComprobanteVendedorMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DocumentoElectronicoService {

  private static final Map<String, TipoDocumento> TIPOS =
      Map.of(
          "notas-credito", new TipoDocumento("NOTA_CREDITO", TiposComprobanteSri.NOTA_CREDITO),
          "notas-debito", new TipoDocumento("NOTA_DEBITO", TiposComprobanteSri.NOTA_DEBITO),
          "guias", new TipoDocumento("GUIA_REMISION", TiposComprobanteSri.GUIA_REMISION),
          "retenciones", new TipoDocumento("RETENCION", TiposComprobanteSri.RETENCION),
          "liquidaciones", new TipoDocumento("LIQUIDACION_COMPRA", TiposComprobanteSri.LIQUIDACION_COMPRA));

  private final PuntoEmisionRepository puntoEmisionRepository;
  private final ComprobanteRepository comprobanteRepository;
  private final SecuencialService secuencialService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional
  public ComprobanteResponse crearBorrador(
      UUID empresaId, String recurso, DocumentoElectronicoRequest request, UsuarioPrincipal principal) {
    TipoDocumento tipo = tipoDesdeRecurso(recurso);
    PuntoEmision punto =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(request.puntoEmisionId(), empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Punto de emision no encontrado"));
    validarPuntoActivo(punto);
    Empresa empresa = punto.getEmpresa();
    long sec = secuencialService.reservarSiguiente(empresaId, punto.getId(), tipo.codigoSri(), principal);
    String secuencial9 = String.format("%09d", sec);
    String claveAcceso =
        ClaveAccesoGenerator.generar(
            request.fechaEmisionOrToday(),
            tipo.codigoSri(),
            empresa.getRuc(),
            empresa.getAmbienteSri(),
            empresa.getTipoEmision(),
            punto.getEstablecimiento().getCodigo(),
            punto.getCodigo(),
            sec,
            ClaveAccesoGenerator.ochoDigitosAleatorios());

    Comprobante c = new Comprobante();
    c.setEmpresa(empresa);
    c.setTipo(tipo.nombre());
    c.setTipoCodigo(tipo.codigoSri());
    c.setEstablecimientoCodigo(punto.getEstablecimiento().getCodigo());
    c.setPuntoEmisionCodigo(punto.getCodigo());
    c.setSecuencial(secuencial9);
    c.setClaveAcceso(claveAcceso);
    c.setFechaEmision(request.fechaEmisionOrToday());
    c.setRazonSocialReceptor(request.razonSocialReceptor());
    c.setIdentificacionReceptor(request.identificacionReceptor());
    c.setSubtotalSinImpuestos(request.subtotalSinImpuestos());
    c.setDescuentoTotal(request.descuentoTotal());
    c.setIvaTotal(request.ivaTotal());
    c.setValorTotal(request.valorTotal());
    c.setAmbienteSri(empresa.getAmbienteSri());
    c.setTipoEmision(empresa.getTipoEmision());
    c.setEstadoSri("BORRADOR");
    c.setOrigen("WEB");
    c.setCustomData(request.safeCustomData());
    c.setUsuarioCreacion(principal.getEmail());
    Comprobante saved = comprobanteRepository.save(c);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  private static TipoDocumento tipoDesdeRecurso(String recurso) {
    TipoDocumento tipo = TIPOS.get(recurso);
    if (tipo == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de documento no soportado");
    }
    return tipo;
  }

  private static void validarPuntoActivo(PuntoEmision punto) {
    if (!"ACTIVO".equals(punto.getEstado()) || !"ACTIVO".equals(punto.getEstablecimiento().getEstado())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "El establecimiento o punto de emision se encuentra inactivo");
    }
  }

  private static ComprobanteResponse toResponse(Comprobante c) {
    return new ComprobanteResponse(
        c.getId(),
        c.getEmpresa().getId(),
        c.getTipo(),
        c.getTipoCodigo(),
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial(),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getRazonSocialReceptor(),
        c.getIdentificacionReceptor(),
        c.getSubtotalSinImpuestos(),
        c.getDescuentoTotal(),
        c.getIvaTotal(),
        c.getValorTotal(),
        c.getEstadoSri(),
        c.getNumeroAutorizacion(),
        c.getFechaAutorizacion(),
        null,
        ComprobanteVendedorMapper.vendedorId(c),
        ComprobanteVendedorMapper.vendedorNombre(c),
        c.getCustomData(),
        List.of());
  }

  private record TipoDocumento(String nombre, String codigoSri) {}
}
