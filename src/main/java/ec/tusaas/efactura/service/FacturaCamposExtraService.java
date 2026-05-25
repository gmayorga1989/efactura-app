package ec.tusaas.efactura.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.tusaas.efactura.dto.factura.FacturaCampoExtraItem;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import java.util.ArrayList;
import java.util.HashMap;
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
public class FacturaCamposExtraService {

  public static final String CONFIG_KEY = "facturaCamposExtra";

  private final EmpresaRepository empresaRepository;
  private final ObjectMapper objectMapper;

  @Transactional(readOnly = true)
  public List<FacturaCampoExtraItem> listar(UUID empresaId) {
    Empresa e = empresaRepository.findById(empresaId).orElseThrow(() -> notFound());
    return parse(e.getConfigExtra().get(CONFIG_KEY));
  }

  @Transactional
  public void reemplazar(UUID empresaId, List<FacturaCampoExtraItem> items) {
    validar(items);
    Empresa e = empresaRepository.findById(empresaId).orElseThrow(() -> notFound());
    Map<String, Object> extra = new HashMap<>(e.getConfigExtra());
    extra.put(CONFIG_KEY, new ArrayList<>(items));
    e.setConfigExtra(extra);
    empresaRepository.save(e);
  }

  private void validar(List<FacturaCampoExtraItem> items) {
    if (items.size() > 40) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Máximo 40 campos extra");
    }
    for (FacturaCampoExtraItem it : items) {
      if ("select".equals(it.tipo()) && (it.opciones() == null || it.opciones().isEmpty())) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Los campos tipo select requieren opciones no vacías: " + it.codigo());
      }
    }
  }

  private List<FacturaCampoExtraItem> parse(Object raw) {
    if (raw == null) {
      return List.of();
    }
    try {
      return objectMapper.convertValue(raw, new TypeReference<List<FacturaCampoExtraItem>>() {});
    } catch (IllegalArgumentException ex) {
      return List.of();
    }
  }

  private static ResponseStatusException notFound() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada");
  }
}
