package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.comprobante.ComprobanteMonitorResponse;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.util.ComprobanteVendedorMapper;
import ec.tusaas.efactura.entity.ComprobanteLogSri;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.NotificacionEmailRepository;
import jakarta.persistence.criteria.Predicate;
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
public class ComprobanteMonitorService {

  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteLogSriRepository comprobanteLogSriRepository;
  private final NotificacionEmailRepository notificacionEmailRepository;

  @Transactional(readOnly = true)
  public Page<ComprobanteMonitorResponse> buscar(
      UUID empresaId,
      String tipoComprobante,
      String estadoSri,
      LocalDate fechaDesde,
      LocalDate fechaHasta,
      String establecimiento,
      String puntoEmision,
      String identificacion,
      String claveAcceso,
      String secuencial,
      Pageable pageable) {
    Pageable sorted =
        pageable.getSort().isSorted()
            ? pageable
            : org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                org.springframework.data.domain.Sort.by(
                    org.springframework.data.domain.Sort.Direction.DESC,
                    "fechaEmision",
                    "fechaCreacion"));
    return comprobanteRepository
        .findAll(
            filtros(
                empresaId,
                tipoComprobante,
                estadoSri,
                fechaDesde,
                fechaHasta,
                establecimiento,
                puntoEmision,
                identificacion,
                claveAcceso,
                secuencial),
            sorted)
        .map(this::toResponse);
  }

  private static Specification<Comprobante> filtros(
      UUID empresaId,
      String tipoComprobante,
      String estadoSri,
      LocalDate fechaDesde,
      LocalDate fechaHasta,
      String establecimiento,
      String puntoEmision,
      String identificacion,
      String claveAcceso,
      String secuencial) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));
      predicates.add(cb.notEqual(root.get("estado"), "ELIMINADO"));
      if (hasText(tipoComprobante)) {
        predicates.add(
            cb.or(
                cb.equal(cb.upper(root.get("tipo")), tipoComprobante.trim().toUpperCase()),
                cb.equal(root.get("tipoCodigo"), tipoComprobante.trim())));
      }
      if (hasText(estadoSri)) {
        predicates.add(cb.equal(cb.upper(root.get("estadoSri")), estadoSri.trim().toUpperCase()));
      }
      if (fechaDesde != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("fechaEmision"), fechaDesde));
      }
      if (fechaHasta != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("fechaEmision"), fechaHasta));
      }
      if (hasText(establecimiento)) {
        predicates.add(cb.equal(root.get("establecimientoCodigo"), establecimiento.trim()));
      }
      if (hasText(puntoEmision)) {
        predicates.add(cb.equal(root.get("puntoEmisionCodigo"), puntoEmision.trim()));
      }
      if (hasText(identificacion)) {
        predicates.add(cb.like(root.get("identificacionReceptor"), "%" + identificacion.trim() + "%"));
      }
      if (hasText(claveAcceso)) {
        predicates.add(cb.like(root.get("claveAcceso"), "%" + claveAcceso.trim() + "%"));
      }
      if (hasText(secuencial)) {
        predicates.add(cb.like(root.get("secuencial"), "%" + secuencial.trim() + "%"));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private ComprobanteMonitorResponse toResponse(Comprobante c) {
    return new ComprobanteMonitorResponse(
        c.getId(),
        c.getEmpresa().getId(),
        c.getTipo(),
        c.getTipoCodigo(),
        c.getEstablecimientoCodigo(),
        c.getPuntoEmisionCodigo(),
        c.getSecuencial(),
        c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial(),
        c.getClaveAcceso(),
        c.getFechaEmision(),
        c.getIdentificacionReceptor(),
        c.getRazonSocialReceptor(),
        c.getValorTotal(),
        c.getEstadoSri(),
        c.getNumeroAutorizacion(),
        c.getFechaAutorizacion(),
        ultimoMensajeSri(c.getId()),
        ComprobanteNotificacionService.resolverEmailReceptor(c),
        estadoEnvioCorreo(c.getId()),
        ComprobanteVendedorMapper.vendedorNombre(c));
  }

  private String estadoEnvioCorreo(UUID comprobanteId) {
    return notificacionEmailRepository
        .findLatestComprobanteCliente(comprobanteId.toString())
        .map(n -> n.getEstado() == null ? "" : n.getEstado().trim())
        .orElse(null);
  }

  private String ultimoMensajeSri(UUID comprobanteId) {
    return comprobanteLogSriRepository
        .findFirstByComprobante_IdOrderByFechaDesc(comprobanteId)
        .map(ComprobanteLogSri::getErrorMensaje)
        .filter(m -> m != null && !m.isBlank())
        .orElse(null);
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
