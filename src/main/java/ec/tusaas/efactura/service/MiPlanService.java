package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.plan.MiPlanResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MiPlanService {

  private final EmpresaRepository empresaRepository;
  private final ComprobanteRepository comprobanteRepository;

  @Transactional(readOnly = true)
  public MiPlanResponse resumen(UUID empresaId) {
    Empresa e = empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    LocalDate hoy = LocalDate.now();
    LocalDate desde = hoy.withDayOfMonth(1);
    LocalDate hasta = hoy.withDayOfMonth(hoy.lengthOfMonth());
    long emitidos = comprobanteRepository.countByEmpresa_IdAndFechaEmisionBetween(empresaId, desde, hasta);
    Integer lim = e.getPlanLimiteMes();
    boolean sinLimite = lim == null;
    Object mod = e.getConfigExtra().get("modulosActivos");
    Map<String, Object> modulos = new HashMap<>();
    if (mod instanceof Map<?, ?> m) {
      for (var entry : m.entrySet()) {
        modulos.put(String.valueOf(entry.getKey()), entry.getValue());
      }
    }
    return new MiPlanResponse(
        e.getPlanCodigo(),
        lim,
        emitidos,
        desde.toString(),
        hasta.toString(),
        sinLimite,
        modulos);
  }
}
