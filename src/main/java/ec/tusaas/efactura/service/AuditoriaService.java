package ec.tusaas.efactura.service;

import ec.tusaas.efactura.entity.Auditoria;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.AuditoriaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final AuditoriaRepository auditoriaRepository;

  @PersistenceContext private EntityManager entityManager;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void registrar(
      String accion,
      UUID empresaId,
      String usuarioEmail,
      String entidad,
      UUID entidadId,
      Map<String, Object> cambios) {
    Auditoria a = new Auditoria();
    if (empresaId != null) {
      a.setEmpresa(entityManager.getReference(Empresa.class, empresaId));
    }
    a.setUsuario(usuarioEmail);
    a.setAccion(accion);
    a.setEntidad(entidad);
    a.setEntidadId(entidadId);
    if (cambios != null && !cambios.isEmpty()) {
      a.setCambios(cambios);
    }
    HttpServletRequest req = currentRequest();
    if (req != null) {
      a.setIp(req.getRemoteAddr());
      String ua = req.getHeader("User-Agent");
      if (ua != null && ua.length() > 500) {
        ua = ua.substring(0, 500);
      }
      a.setUserAgent(ua);
    }
    auditoriaRepository.save(a);
  }

  private static HttpServletRequest currentRequest() {
    RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
    if (attrs instanceof ServletRequestAttributes sra) {
      return sra.getRequest();
    }
    return null;
  }
}
