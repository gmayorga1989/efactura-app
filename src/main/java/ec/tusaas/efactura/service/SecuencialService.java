package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.tributario.SecuencialResponse;
import ec.tusaas.efactura.entity.PuntoEmision;
import ec.tusaas.efactura.entity.Secuencial;
import ec.tusaas.efactura.repository.PuntoEmisionRepository;
import ec.tusaas.efactura.repository.SecuencialRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SecuencialService {

  private final SecuencialRepository secuencialRepository;
  private final PuntoEmisionRepository puntoEmisionRepository;
  private final EmpresaTributarioService empresaTributarioService;

  @Transactional(readOnly = true)
  public List<SecuencialResponse> listar(UUID empresaId, UUID puntoEmisionId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    puntoEmisionRepository
        .findByIdAndEmpresa_Id(puntoEmisionId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return secuencialRepository.findByPuntoEmision_IdOrderByTipoComprobanteAsc(puntoEmisionId).stream()
        .map(this::toResponse)
        .toList();
  }

  /**
   * Reserva el siguiente valor para el tipo de comprobante (bloqueo pesimista). Pensado para uso en el pipeline de
   * emisión (Fase 3).
   */
  @Transactional
  public long reservarSiguiente(UUID empresaId, UUID puntoEmisionId, String tipoComprobante, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    PuntoEmision pe =
        puntoEmisionRepository
            .findByIdAndEmpresa_Id(puntoEmisionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    Secuencial s =
        secuencialRepository
            .findForUpdate(pe.getId(), tipoComprobante)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Secuencial no configurado"));
    if (!s.getEmpresa().getId().equals(empresaId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    s.setValorActual(s.getValorActual() + 1);
    secuencialRepository.save(s);
    return s.getValorActual();
  }

  private SecuencialResponse toResponse(Secuencial s) {
    return new SecuencialResponse(
        s.getId(), s.getPuntoEmision().getId(), s.getTipoComprobante(), s.getValorActual(), s.getEstado());
  }
}
