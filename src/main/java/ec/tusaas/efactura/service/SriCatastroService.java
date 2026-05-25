package ec.tusaas.efactura.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ec.tusaas.efactura.config.props.SriProperties;
import ec.tusaas.efactura.dto.maestro.ClienteDireccionResponse;
import ec.tusaas.efactura.dto.maestro.RucConsultaResponse;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class SriCatastroService {

  private final SriProperties sriProperties;
  private final RestClient.Builder restClientBuilder;
  private final ObjectMapper objectMapper;

  public RucConsultaResponse consultarRuc(String ruc) {
    String normalized = normalizeRuc(ruc);
    RestClient client = restClientBuilder.baseUrl(sriProperties.getCatastroBaseUrl()).build();
    JsonNode contribuyentes =
        get(client, "/ConsolidadoContribuyente/obtenerPorNumerosRuc?ruc={ruc}", normalized);
    JsonNode establecimientos =
        get(client, "/Establecimiento/consultarPorNumeroRuc?numeroRuc={ruc}", normalized);

    JsonNode contribuyente = first(contribuyentes);
    List<Map<String, Object>> establecimientosRaw = toList(establecimientos);
    List<ClienteDireccionResponse> direcciones =
        establecimientosRaw.stream().map(SriCatastroService::toDireccion).toList();
    return new RucConsultaResponse(
        normalized,
        contribuyente != null,
        text(contribuyente, "razonSocial"),
        firstNonBlank(
            establecimientosRaw.stream()
                .map(row -> string(row.get("nombreFantasiaComercial")))
                .filter(SriCatastroService::hasText)
                .findFirst()
                .orElse(null),
            text(contribuyente, "nombreFantasiaComercial")),
        text(contribuyente, "tipoContribuyente"),
        text(contribuyente, "estadoContribuyenteRuc"),
        text(contribuyente, "obligadoLlevarContabilidad"),
        text(contribuyente, "contribuyenteEspecial"),
        text(contribuyente, "regimen"),
        text(contribuyente, "actividadEconomicaPrincipal"),
        direcciones,
        toMap(contribuyente),
        establecimientosRaw);
  }

  private JsonNode get(RestClient client, String uri, String ruc) {
    try {
      return client.get().uri(uri, ruc).retrieve().body(JsonNode.class);
    } catch (RestClientException e) {
      log.warn("[sri-catastro] error consultando uri={} ruc={}: {}", uri, ruc, e.getMessage());
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No fue posible consultar catastro SRI");
    }
  }

  private static ClienteDireccionResponse toDireccion(Map<String, Object> row) {
    String tipo = string(row.get("tipoEstablecimiento"));
    String numero = string(row.get("numeroEstablecimiento"));
    return new ClienteDireccionResponse(
        null,
        hasText(tipo) ? tipo.toUpperCase(Locale.ROOT) : "SUCURSAL",
        string(row.get("direccionCompleta")),
        null,
        null,
        null,
        hasText(numero) ? "Establecimiento " + numero : null,
        "SI".equalsIgnoreCase(string(row.get("matriz"))),
        string(row.get("estado")));
  }

  private static JsonNode first(JsonNode node) {
    return node != null && node.isArray() && !node.isEmpty() ? node.get(0) : null;
  }

  private Map<String, Object> toMap(JsonNode node) {
    if (node == null || node.isNull()) {
      return Map.of();
    }
    return objectMapper.convertValue(node, new TypeReference<>() {});
  }

  private List<Map<String, Object>> toList(JsonNode node) {
    if (node == null || !node.isArray()) {
      return List.of();
    }
    return objectMapper.convertValue(node, new TypeReference<>() {});
  }

  private static String text(JsonNode node, String field) {
    if (node == null || !node.hasNonNull(field)) {
      return null;
    }
    return string(node.get(field).asText());
  }

  private static String normalizeRuc(String ruc) {
    String normalized = string(ruc);
    if (normalized == null || !normalized.matches("\\d{13}")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RUC debe tener 13 digitos");
    }
    return normalized;
  }

  private static String string(Object value) {
    if (value == null) {
      return null;
    }
    String text = value.toString().trim();
    return text.isEmpty() || "null".equalsIgnoreCase(text) ? null : text;
  }

  private static String firstNonBlank(String first, String second) {
    return hasText(first) ? first : second;
  }

  private static boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }
}
