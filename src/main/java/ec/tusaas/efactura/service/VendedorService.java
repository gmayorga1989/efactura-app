package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.vendedor.VendedorKpiResponse;
import ec.tusaas.efactura.dto.vendedor.VendedorMetaRequest;
import ec.tusaas.efactura.dto.vendedor.VendedorMetaResponse;
import ec.tusaas.efactura.dto.vendedor.VendedorRequest;
import ec.tusaas.efactura.dto.vendedor.VendedorResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.Vendedor;
import ec.tusaas.efactura.entity.VendedorMeta;
import ec.tusaas.efactura.repository.CotizacionRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.VendedorMetaRepository;
import ec.tusaas.efactura.repository.VendedorRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class VendedorService {

  private final EmpresaRepository empresaRepository;
  private final VendedorRepository vendedorRepository;
  private final VendedorMetaRepository vendedorMetaRepository;
  private final CotizacionRepository cotizacionRepository;

  @Transactional(readOnly = true)
  public Page<VendedorResponse> listar(UUID empresaId, String estado, Pageable pageable) {
    Page<Vendedor> page =
        estado != null && !estado.isBlank()
            ? vendedorRepository.findByEmpresa_IdAndEstadoOrderByNombresAsc(empresaId, estado, pageable)
            : vendedorRepository.findByEmpresa_IdOrderByNombresAsc(empresaId, pageable);
    return page.map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public List<VendedorResponse> listarActivos(UUID empresaId) {
    return vendedorRepository.findByEmpresa_IdAndEstadoOrderByNombresAsc(empresaId, "ACTIVO").stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public VendedorResponse obtener(UUID empresaId, UUID id) {
    return toResponse(load(empresaId, id));
  }

  @Transactional
  public VendedorResponse crear(UUID empresaId, VendedorRequest body, UsuarioPrincipal principal) {
    Empresa empresa = loadEmpresa(empresaId);
    validarCodigoUnico(empresaId, body.codigo(), null);
    Vendedor v = new Vendedor();
    v.setEmpresa(empresa);
    aplicar(v, body);
    v.setUsuarioCreacion(principal.getEmail());
    return toResponse(vendedorRepository.save(v));
  }

  @Transactional
  public VendedorResponse actualizar(UUID empresaId, UUID id, VendedorRequest body, UsuarioPrincipal principal) {
    Vendedor v = load(empresaId, id);
    validarCodigoUnico(empresaId, body.codigo(), id);
    aplicar(v, body);
    v.setUsuarioModificacion(principal.getEmail());
    v.setFechaModificacion(java.time.Instant.now());
    return toResponse(vendedorRepository.save(v));
  }

  @Transactional
  public VendedorMetaResponse guardarMeta(UUID empresaId, UUID vendedorId, VendedorMetaRequest body, UsuarioPrincipal principal) {
    Vendedor v = load(empresaId, vendedorId);
    VendedorMeta meta =
        vendedorMetaRepository
            .findByVendedor_IdAndPeriodoAnioAndPeriodoMes(vendedorId, body.periodoAnio(), body.periodoMes())
            .orElseGet(VendedorMeta::new);
    meta.setVendedor(v);
    meta.setEmpresa(v.getEmpresa());
    meta.setPeriodoAnio(body.periodoAnio());
    meta.setPeriodoMes(body.periodoMes());
    meta.setMetaMonto(body.metaMonto());
    meta.setMetaCantidad(body.metaCantidad());
    meta.setNotas(body.notas());
    if (meta.getUsuarioCreacion() == null) {
      meta.setUsuarioCreacion(principal.getEmail());
    }
    return toMetaResponse(vendedorMetaRepository.save(meta));
  }

  @Transactional(readOnly = true)
  public List<VendedorMetaResponse> listarMetas(UUID empresaId, UUID vendedorId, int anio) {
    load(empresaId, vendedorId);
    return vendedorMetaRepository.findByVendedor_IdAndPeriodoAnioOrderByPeriodoMesAsc(vendedorId, anio).stream()
        .map(this::toMetaResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<VendedorKpiResponse> kpis(UUID empresaId, int anio, int mes) {
    YearMonth ym = YearMonth.of(anio, mes);
    LocalDate desde = ym.atDay(1);
    LocalDate hasta = ym.atEndOfMonth();
    List<VendedorKpiResponse> out = new ArrayList<>();
    for (Vendedor v : vendedorRepository.findByEmpresa_IdAndEstadoOrderByNombresAsc(empresaId, "ACTIVO")) {
      BigDecimal meta =
          vendedorMetaRepository
              .findByVendedor_IdAndPeriodoAnioAndPeriodoMes(v.getId(), anio, mes)
              .map(VendedorMeta::getMetaMonto)
              .orElse(BigDecimal.ZERO);
      BigDecimal ventas = cotizacionRepository.sumarVentasVendedor(empresaId, v.getId(), desde, hasta);
      long conv = cotizacionRepository.contarConversionesVendedor(empresaId, v.getId(), desde, hasta);
      BigDecimal pct =
          meta.signum() > 0
              ? ventas.multiply(BigDecimal.valueOf(100)).divide(meta, 2, RoundingMode.HALF_UP)
              : BigDecimal.ZERO;
      out.add(
          new VendedorKpiResponse(
              v.getId(),
              nombreCompleto(v),
              anio,
              mes,
              meta,
              ventas,
              conv,
              pct));
    }
    return out;
  }

  private void validarCodigoUnico(UUID empresaId, String codigo, UUID excludeId) {
    if (codigo == null || codigo.isBlank()) {
      return;
    }
    String c = codigo.trim();
    boolean dup =
        excludeId == null
            ? vendedorRepository.existsByEmpresa_IdAndCodigoIgnoreCase(empresaId, c)
            : vendedorRepository.existsByEmpresa_IdAndCodigoIgnoreCaseAndIdNot(empresaId, c, excludeId);
    if (dup) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un vendedor con ese código");
    }
  }

  private void aplicar(Vendedor v, VendedorRequest body) {
    v.setCodigo(body.codigo() != null ? body.codigo().trim() : null);
    v.setNombres(body.nombres().trim());
    v.setApellidos(body.apellidos() != null ? body.apellidos().trim() : null);
    v.setEmail(body.email());
    v.setTelefono(body.telefono());
    v.setDocumentoIdentidad(body.documentoIdentidad());
    v.setNotas(body.notas());
    if (body.estado() != null && !body.estado().isBlank()) {
      v.setEstado(body.estado().trim().toUpperCase());
    }
  }

  private Vendedor load(UUID empresaId, UUID id) {
    return vendedorRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));
  }

  private Empresa loadEmpresa(UUID empresaId) {
    return empresaRepository
        .findById(empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
  }

  private VendedorResponse toResponse(Vendedor v) {
    return new VendedorResponse(
        v.getId(),
        v.getCodigo(),
        v.getNombres(),
        v.getApellidos(),
        nombreCompleto(v),
        v.getEmail(),
        v.getTelefono(),
        v.getDocumentoIdentidad(),
        v.getNotas(),
        v.getEstado());
  }

  private VendedorMetaResponse toMetaResponse(VendedorMeta m) {
    return new VendedorMetaResponse(
        m.getId(), m.getPeriodoAnio(), m.getPeriodoMes(), m.getMetaMonto(), m.getMetaCantidad(), m.getNotas());
  }

  private String nombreCompleto(Vendedor v) {
    String n = v.getNombres() != null ? v.getNombres().trim() : "";
    String a = v.getApellidos() != null ? v.getApellidos().trim() : "";
    return (n + " " + a).trim();
  }
}
