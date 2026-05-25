package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.maestro.ListaPrecioCreateRequest;
import ec.tusaas.efactura.dto.maestro.ListaPrecioResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.ListaPrecio;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.repository.ListaPrecioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ListaPrecioService {

  private final ListaPrecioRepository listaPrecioRepository;
  private final EmpresaRepository empresaRepository;

  @Transactional(readOnly = true)
  public List<ListaPrecioResponse> listarActivas(UUID empresaId) {
    empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return listaPrecioRepository.findByEmpresa_IdAndEstadoOrderByCodigoAsc(empresaId, "ACTIVO").stream()
        .map(lp -> new ListaPrecioResponse(lp.getId(), lp.getCodigo(), lp.getNombre(), lp.isEsDefault()))
        .toList();
  }

  @Transactional
  public ListaPrecioResponse crear(UUID empresaId, ListaPrecioCreateRequest req) {
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    String codigo = req.codigo().trim().toUpperCase(Locale.ROOT);
    String nombre = req.nombre().trim();
    if (codigo.isEmpty() || nombre.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo y nombre son obligatorios");
    }
    if (listaPrecioRepository.findByEmpresa_IdAndCodigo(empresaId, codigo).isPresent()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una lista con ese codigo");
    }
    boolean marcarDefault = Boolean.TRUE.equals(req.esDefault());
    if (marcarDefault) {
      for (ListaPrecio lp : listaPrecioRepository.findByEmpresa_IdAndEstadoOrderByCodigoAsc(empresaId, "ACTIVO")) {
        if (lp.isEsDefault()) {
          lp.setEsDefault(false);
          listaPrecioRepository.save(lp);
        }
      }
    }
    ListaPrecio lp = new ListaPrecio();
    lp.setEmpresa(empresa);
    lp.setCodigo(codigo);
    lp.setNombre(nombre);
    lp.setEsDefault(marcarDefault);
    lp.setEstado("ACTIVO");
    lp.setFechaCreacion(Instant.now());
    ListaPrecio saved = listaPrecioRepository.save(lp);
    return new ListaPrecioResponse(saved.getId(), saved.getCodigo(), saved.getNombre(), saved.isEsDefault());
  }

  /** Garantiza la lista BASE (p. ej. tras datos legacy sin migración). */
  @Transactional
  public ListaPrecio asegurarListaBase(UUID empresaId) {
    return listaPrecioRepository
        .findByEmpresa_IdAndCodigo(empresaId, "BASE")
        .orElseGet(
            () -> {
              Empresa empresa =
                  empresaRepository
                      .findById(empresaId)
                      .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
              ListaPrecio lp = new ListaPrecio();
              lp.setEmpresa(empresa);
              lp.setCodigo("BASE");
              lp.setNombre("Lista base");
              lp.setEsDefault(true);
              lp.setEstado("ACTIVO");
              lp.setFechaCreacion(Instant.now());
              return listaPrecioRepository.save(lp);
            });
  }
}
