package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.reporte.ComprobanteTipoEstadoConteo;
import ec.tusaas.efactura.dto.reporte.EstadoSriConteo;
import ec.tusaas.efactura.dto.reporte.ReporteDocumentoResponse;
import ec.tusaas.efactura.dto.reporte.ReporteDocumentosResponse;
import ec.tusaas.efactura.dto.reporte.ReporteGuiaResponse;
import ec.tusaas.efactura.dto.reporte.ReporteGuiasResponse;
import ec.tusaas.efactura.dto.reporte.ReporteResumenResponse;
import ec.tusaas.efactura.dto.reporte.ReporteRetencionLineaResponse;
import ec.tusaas.efactura.dto.reporte.ReporteRetencionResponse;
import ec.tusaas.efactura.dto.reporte.ReporteRetencionesResponse;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteGuia;
import ec.tusaas.efactura.entity.ComprobanteRetencion;
import ec.tusaas.efactura.repository.ComprobanteGuiaRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.ComprobanteRetencionRepository;
import ec.tusaas.efactura.tributario.TiposComprobanteSri;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReporteService {

  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteGuiaRepository comprobanteGuiaRepository;
  private final ComprobanteRetencionRepository comprobanteRetencionRepository;

  @Transactional(readOnly = true)
  public ReporteDocumentosResponse ventas(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision,
      Pageable pageable) {
    List<String> tipos =
        List.of(TiposComprobanteSri.FACTURA, TiposComprobanteSri.NOTA_CREDITO, TiposComprobanteSri.NOTA_DEBITO);
    Page<Comprobante> page =
        comprobanteRepository.findAll(
            filtros(empresaId, desde, hasta, tipos, estadoSri, identificacion, establecimiento, puntoEmision),
            pageable);
    return new ReporteDocumentosResponse(
        empresaId,
        desde,
        hasta,
        resumenVentas(empresaId, desde, hasta),
        estados(empresaId, desde, hasta, ""),
        tiposEstado(empresaId, desde, hasta, ""),
        page.map(ReporteService::documento));
  }

  @Transactional(readOnly = true)
  public ReporteDocumentosResponse liquidaciones(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision,
      Pageable pageable) {
    return documentosPorTipo(
        empresaId,
        desde,
        hasta,
        TiposComprobanteSri.LIQUIDACION_COMPRA,
        estadoSri,
        identificacion,
        establecimiento,
        puntoEmision,
        pageable);
  }

  @Transactional(readOnly = true)
  public ReporteGuiasResponse guias(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision,
      Pageable pageable) {
    Page<Comprobante> page =
        comprobanteRepository.findAll(
            filtros(
                empresaId,
                desde,
                hasta,
                List.of(TiposComprobanteSri.GUIA_REMISION),
                estadoSri,
                identificacion,
                establecimiento,
                puntoEmision),
            pageable);
    return new ReporteGuiasResponse(
        empresaId,
        desde,
        hasta,
        page.getTotalElements(),
        estados(empresaId, desde, hasta, TiposComprobanteSri.GUIA_REMISION),
        page.map(this::guia));
  }

  @Transactional(readOnly = true)
  public ReporteRetencionesResponse retenciones(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision,
      Pageable pageable) {
    Page<Comprobante> page =
        comprobanteRepository.findAll(
            filtros(
                empresaId,
                desde,
                hasta,
                List.of(TiposComprobanteSri.RETENCION),
                estadoSri,
                identificacion,
                establecimiento,
                puntoEmision),
            pageable);
    return new ReporteRetencionesResponse(
        empresaId,
        desde,
        hasta,
        page.getTotalElements(),
        safe(comprobanteRetencionRepository.sumarBaseImponible(empresaId, desde, hasta)),
        safe(comprobanteRetencionRepository.sumarValorRetenido(empresaId, desde, hasta)),
        estados(empresaId, desde, hasta, TiposComprobanteSri.RETENCION),
        page.map(this::retencion));
  }

  private ReporteDocumentosResponse documentosPorTipo(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      String tipoCodigo,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision,
      Pageable pageable) {
    Page<Comprobante> page =
        comprobanteRepository.findAll(
            filtros(empresaId, desde, hasta, List.of(tipoCodigo), estadoSri, identificacion, establecimiento, puntoEmision),
            pageable);
    return new ReporteDocumentosResponse(
        empresaId,
        desde,
        hasta,
        resumenSimple(empresaId, desde, hasta, tipoCodigo),
        estados(empresaId, desde, hasta, tipoCodigo),
        tiposEstado(empresaId, desde, hasta, tipoCodigo),
        page.map(ReporteService::documento));
  }

  private ReporteResumenResponse resumenVentas(UUID empresaId, LocalDate desde, LocalDate hasta) {
    BigDecimal facturas = safe(comprobanteRepository.sumarTotalPorTipoCodigo(empresaId, desde, hasta, TiposComprobanteSri.FACTURA));
    BigDecimal nc = safe(comprobanteRepository.sumarTotalPorTipoCodigo(empresaId, desde, hasta, TiposComprobanteSri.NOTA_CREDITO));
    BigDecimal nd = safe(comprobanteRepository.sumarTotalPorTipoCodigo(empresaId, desde, hasta, TiposComprobanteSri.NOTA_DEBITO));
    long total =
        comprobanteRepository
            .findAll(
                filtros(
                    empresaId,
                    desde,
                    hasta,
                    List.of(TiposComprobanteSri.FACTURA, TiposComprobanteSri.NOTA_CREDITO, TiposComprobanteSri.NOTA_DEBITO),
                    null,
                    null,
                    null,
                    null),
                Pageable.unpaged())
            .getTotalElements();
    return new ReporteResumenResponse(
        total,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        facturas.add(nd).subtract(nc),
        facturas,
        nc,
        nd,
        facturas.add(nd).subtract(nc));
  }

  private ReporteResumenResponse resumenSimple(UUID empresaId, LocalDate desde, LocalDate hasta, String tipoCodigo) {
    BigDecimal total = safe(comprobanteRepository.sumarTotalPorTipoCodigo(empresaId, desde, hasta, tipoCodigo));
    long docs =
        comprobanteRepository
            .findAll(filtros(empresaId, desde, hasta, List.of(tipoCodigo), null, null, null, null), Pageable.unpaged())
            .getTotalElements();
    return new ReporteResumenResponse(docs, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, total, total, BigDecimal.ZERO, BigDecimal.ZERO, total);
  }

  private List<EstadoSriConteo> estados(UUID empresaId, LocalDate desde, LocalDate hasta, String tipo) {
    return tipo == null || tipo.isBlank()
        ? comprobanteRepository.contarPorEstadoSri(empresaId, desde, hasta)
        : comprobanteRepository.contarPorEstadoSriConTipo(empresaId, desde, hasta, tipo);
  }

  private List<ComprobanteTipoEstadoConteo> tiposEstado(UUID empresaId, LocalDate desde, LocalDate hasta, String tipo) {
    return comprobanteRepository.contarPorTipoYEstado(empresaId, desde, hasta, tipo == null ? "" : tipo);
  }

  private ReporteGuiaResponse guia(Comprobante c) {
    ComprobanteGuia g = comprobanteGuiaRepository.findByComprobante_Id(c.getId()).stream().findFirst().orElse(null);
    return new ReporteGuiaResponse(
        c.getId(),
        numero(c),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getEstadoSri(),
        c.getRazonSocialReceptor(),
        c.getIdentificacionReceptor(),
        g == null ? null : g.getDireccionPartida(),
        g == null ? null : g.getDireccionDestino(),
        g == null ? null : g.getMotivoTraslado(),
        g == null ? null : g.getRazonSocialTransportista(),
        g == null ? null : g.getRucTransportista(),
        g == null ? null : g.getPlaca(),
        g == null ? null : g.getFechaInicioTransporte(),
        g == null ? null : g.getFechaFinTransporte());
  }

  private ReporteRetencionResponse retencion(Comprobante c) {
    List<ComprobanteRetencion> rows = comprobanteRetencionRepository.findByComprobante_Id(c.getId());
    BigDecimal base = rows.stream().map(ComprobanteRetencion::getBaseImponible).map(ReporteService::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal valor = rows.stream().map(ComprobanteRetencion::getValor).map(ReporteService::safe).reduce(BigDecimal.ZERO, BigDecimal::add);
    return new ReporteRetencionResponse(
        c.getId(),
        numero(c),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getEstadoSri(),
        c.getRazonSocialReceptor(),
        c.getIdentificacionReceptor(),
        base,
        valor,
        rows.stream()
            .map(
                r ->
                    new ReporteRetencionLineaResponse(
                        r.getCodigo(),
                        r.getCodigoRetencion(),
                        r.getBaseImponible(),
                        r.getPorcentaje(),
                        r.getValor(),
                        r.getDocumentoSustentoTipo(),
                        r.getDocumentoSustentoNumero(),
                        r.getDocumentoSustentoFecha()))
            .toList());
  }

  private static ReporteDocumentoResponse documento(Comprobante c) {
    return new ReporteDocumentoResponse(
        c.getId(),
        c.getTipo(),
        c.getTipoCodigo(),
        numero(c),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        c.getSubtotalSinImpuestos(),
        c.getDescuentoTotal(),
        c.getIvaTotal(),
        c.getValorTotal(),
        c.getEstadoSri(),
        c.getNumeroAutorizacion(),
        c.getFechaAutorizacion(),
        c.getOrigen());
  }

  private static Specification<Comprobante> filtros(
      UUID empresaId,
      LocalDate desde,
      LocalDate hasta,
      List<String> tiposCodigo,
      String estadoSri,
      String identificacion,
      String establecimiento,
      String puntoEmision) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));
      predicates.add(cb.notEqual(root.get("estado"), "ELIMINADO"));
      predicates.add(root.get("tipoCodigo").in(tiposCodigo));
      predicates.add(cb.between(root.get("fechaEmision"), desde, hasta));
      if (hasText(estadoSri)) {
        predicates.add(cb.equal(cb.upper(root.get("estadoSri")), estadoSri.trim().toUpperCase()));
      }
      if (hasText(identificacion)) {
        predicates.add(cb.like(root.get("identificacionReceptor"), "%" + identificacion.trim() + "%"));
      }
      if (hasText(establecimiento)) {
        predicates.add(cb.equal(root.get("establecimientoCodigo"), establecimiento.trim()));
      }
      if (hasText(puntoEmision)) {
        predicates.add(cb.equal(root.get("puntoEmisionCodigo"), puntoEmision.trim()));
      }
      if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        query.orderBy(cb.desc(root.get("fechaEmision")), cb.desc(root.get("fechaCreacion")));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static String numero(Comprobante c) {
    return c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial();
  }

  private static BigDecimal safe(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
