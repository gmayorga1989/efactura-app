package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ClienteRequest;
import ec.tusaas.efactura.dto.maestro.ClienteResponse;
import ec.tusaas.efactura.dto.maestro.ClienteDireccionRequest;
import ec.tusaas.efactura.dto.maestro.ClienteDireccionResponse;
import ec.tusaas.efactura.entity.Cliente;
import ec.tusaas.efactura.entity.ClienteDireccion;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.ClienteRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {

  private final ClienteRepository clienteRepository;
  private final EmpresaRepository empresaRepository;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public Page<ClienteResponse> listar(UUID empresaId, String tipoTercero, String estado, String q, Pageable pageable) {
    return clienteRepository.findAll(filtros(empresaId, tipoTercero, estado, q), pageable).map(this::toResponse);
  }

  @Transactional(readOnly = true)
  public ClienteResponse obtener(UUID empresaId, UUID id) {
    return toResponse(buscar(empresaId, id));
  }

  @Transactional
  public ClienteResponse crear(UUID empresaId, ClienteRequest req, String tipoTerceroDefault, UsuarioPrincipal principal) {
    String tipoSolicitado = normalizeTipoTercero(hasText(req.tipoTercero()) ? req.tipoTercero() : tipoTerceroDefault);
    var existente = clienteRepository
        .findByEmpresa_IdAndTipoIdentificacionAndIdentificacion(
            empresaId, normalize(req.tipoIdentificacion()), normalize(req.identificacion()));
    if (existente.isPresent()) {
      Cliente c = existente.get();
      aplicar(c, req, mergeTipoTercero(c.getTipoTercero(), tipoSolicitado));
      c.setFechaModificacion(Instant.now());
      c.setUsuarioModificacion(principal.getEmail());
      return toResponse(clienteRepository.save(c));
    }
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Cliente c = new Cliente();
    c.setEmpresa(empresa);
    aplicar(c, req, tipoSolicitado);
    c.setUsuarioCreacion(principal.getEmail());
    Cliente saved = clienteRepository.save(c);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public ClienteResponse actualizar(
      UUID empresaId, UUID id, ClienteRequest req, String tipoTerceroDefault, UsuarioPrincipal principal) {
    Cliente c = buscar(empresaId, id);
    if (!c.getTipoIdentificacion().equals(req.tipoIdentificacion())
        || !c.getIdentificacion().equals(req.identificacion())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se permite cambiar la identificacion del tercero");
    }
    aplicar(c, req, tipoTerceroDefault);
    c.setFechaModificacion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    Cliente saved = clienteRepository.save(c);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public void eliminar(UUID empresaId, UUID id, UsuarioPrincipal principal) {
    Cliente c = buscar(empresaId, id);
    c.setEstado("ELIMINADO");
    c.setFechaModificacion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    clienteRepository.save(c);
    dashboardCacheService.evictEmpresa(empresaId);
  }

  @Transactional
  public ClienteResponse cambiarTipoTercero(
      UUID empresaId, UUID id, String tipoTercero, UsuarioPrincipal principal) {
    Cliente c = buscar(empresaId, id);
    c.setTipoTercero(normalizeTipoTercero(tipoTercero));
    c.setFechaModificacion(Instant.now());
    c.setUsuarioModificacion(principal.getEmail());
    return toResponse(clienteRepository.save(c));
  }

  private Cliente buscar(UUID empresaId, UUID id) {
    return clienteRepository
        .findByIdAndEmpresa_Id(id, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente/proveedor no encontrado"));
  }

  private static void aplicar(Cliente c, ClienteRequest req, String tipoTerceroDefault) {
    validarIdentificacion(req.tipoIdentificacion(), req.identificacion());
    c.setTipoIdentificacion(normalize(req.tipoIdentificacion()));
    c.setIdentificacion(normalize(req.identificacion()));
    c.setRazonSocial(trim(req.razonSocial()));
    c.setNombreComercial(trim(req.nombreComercial()));
    if (hasText(req.tipoTercero())) {
      c.setTipoTercero(normalizeTipoTercero(req.tipoTercero()));
    } else {
      c.setTipoTercero(mergeTipoTercero(c.getTipoTercero(), tipoTerceroDefault));
    }
    c.setDireccion(trim(req.direccion()));
    c.setTelefono(trim(req.telefono()));
    c.setEmail(trimLower(req.email()));
    c.setContactoNombre(trim(req.contactoNombre()));
    c.setContactoTelefono(trim(req.contactoTelefono()));
    c.setContactoEmail(trimLower(req.contactoEmail()));
    c.setObligadoContabilidad(normalizeSiNo(req.obligadoContabilidad()));
    c.setContribuyenteEspecial(trim(req.contribuyenteEspecial()));
    c.setRegimen(trim(req.regimen()));
    c.setEstadoSri(trim(req.estadoSri()));
    c.setActividadEconomica(trim(req.actividadEconomica()));
    c.setFuenteDatos(trim(req.fuenteDatos()));
    c.setCustomData(req.safeCustomData());
    c.setEstado("ACTIVO");
    aplicarDirecciones(c, req);
  }

  private ClienteResponse toResponse(Cliente c) {
    return new ClienteResponse(
        c.getId(),
        c.getEmpresa().getId(),
        c.getTipoIdentificacion(),
        c.getIdentificacion(),
        c.getRazonSocial(),
        c.getNombreComercial(),
        c.getTipoTercero(),
        c.getDireccion(),
        c.getTelefono(),
        c.getEmail(),
        c.getContactoNombre(),
        c.getContactoTelefono(),
        c.getContactoEmail(),
        c.getObligadoContabilidad(),
        c.getContribuyenteEspecial(),
        c.getRegimen(),
        c.getEstadoSri(),
        c.getActividadEconomica(),
        c.getFuenteDatos(),
        c.getDirecciones().stream().map(ClienteService::toDireccionResponse).toList(),
        c.getEstado(),
        c.getCustomData());
  }

  private static Specification<Cliente> filtros(UUID empresaId, String tipoTercero, String estado, String q) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();
      predicates.add(cb.equal(root.get("empresa").get("id"), empresaId));
      predicates.add(cb.notEqual(root.get("estado"), "ELIMINADO"));
      if (hasText(tipoTercero)) {
        String tipo = normalizeTipoTercero(tipoTercero);
        if ("CLIENTE".equals(tipo) || "PROVEEDOR".equals(tipo)) {
          predicates.add(root.get("tipoTercero").in(tipo, "AMBOS"));
        } else {
          predicates.add(cb.equal(root.get("tipoTercero"), tipo));
        }
      }
      if (hasText(estado)) {
        predicates.add(cb.equal(root.get("estado"), normalize(estado)));
      }
      if (hasText(q)) {
        String like = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
        predicates.add(
            cb.or(
                cb.like(cb.upper(root.get("razonSocial")), like),
                cb.like(cb.upper(root.get("identificacion")), like),
                cb.like(cb.upper(root.get("email")), like)));
      }
      if (!Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
        query.orderBy(cb.asc(root.get("razonSocial")));
      }
      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }

  private static void aplicarDirecciones(Cliente c, ClienteRequest req) {
    List<ClienteDireccionRequest> requestDirecciones = req.safeDirecciones();
    c.getDirecciones().clear();
    if (requestDirecciones.isEmpty()) {
      if (hasText(req.direccion())) {
        ClienteDireccion d = new ClienteDireccion();
        d.setCliente(c);
        d.setTipo("MATRIZ");
        d.setDireccion(trim(req.direccion()));
        d.setPrincipal(true);
        c.getDirecciones().add(d);
      }
      return;
    }

    boolean hasPrincipal = requestDirecciones.stream().anyMatch(d -> Boolean.TRUE.equals(d.principal()));
    for (int i = 0; i < requestDirecciones.size(); i++) {
      ClienteDireccionRequest item = requestDirecciones.get(i);
      ClienteDireccion d = new ClienteDireccion();
      d.setCliente(c);
      d.setTipo(hasText(item.tipo()) ? normalize(item.tipo()) : "SUCURSAL");
      d.setDireccion(trim(item.direccion()));
      d.setProvincia(trim(item.provincia()));
      d.setCanton(trim(item.canton()));
      d.setParroquia(trim(item.parroquia()));
      d.setReferencia(trim(item.referencia()));
      d.setPrincipal(Boolean.TRUE.equals(item.principal()) || (!hasPrincipal && i == 0));
      c.getDirecciones().add(d);
      if (d.isPrincipal()) {
        c.setDireccion(d.getDireccion());
      }
    }
  }

  private static ClienteDireccionResponse toDireccionResponse(ClienteDireccion d) {
    return new ClienteDireccionResponse(
        d.getId(),
        d.getTipo(),
        d.getDireccion(),
        d.getProvincia(),
        d.getCanton(),
        d.getParroquia(),
        d.getReferencia(),
        d.isPrincipal(),
        d.getEstado());
  }

  private static void validarIdentificacion(String tipoIdentificacion, String identificacion) {
    String tipo = normalize(tipoIdentificacion);
    String id = normalize(identificacion);
    if ("04".equals(tipo) && !id.matches("\\d{13}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUC debe tener 13 digitos");
    }
    if ("05".equals(tipo) && !id.matches("\\d{10}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cedula debe tener 10 digitos");
    }
    if ("06".equals(tipo) && (id.length() < 3 || id.length() > 20)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pasaporte debe tener entre 3 y 20 caracteres");
    }
  }

  private static String normalizeTipoTercero(String value) {
    String normalized = hasText(value) ? normalize(value) : "CLIENTE";
    if (!List.of("CLIENTE", "PROVEEDOR", "AMBOS").contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "tipoTercero debe ser CLIENTE, PROVEEDOR o AMBOS");
    }
    return normalized;
  }

  private static String mergeTipoTercero(String actual, String solicitado) {
    String a = normalizeTipoTercero(actual);
    String s = normalizeTipoTercero(solicitado);
    if (a.equals(s)) {
      return a;
    }
    if ("AMBOS".equals(a) || "AMBOS".equals(s)) {
      return "AMBOS";
    }
    return "AMBOS";
  }

  private static String normalizeSiNo(String value) {
    if (!hasText(value)) {
      return null;
    }
    String normalized = normalize(value);
    if (!List.of("SI", "NO").contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor SI/NO invalido");
    }
    return normalized;
  }

  private static String normalize(String value) {
    return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
  }

  private static String trimLower(String value) {
    return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
  }

  private static String trim(String value) {
    return value == null ? null : value.trim();
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
