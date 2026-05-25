package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.apikey.ApiKeyCreateRequest;
import ec.tusaas.efactura.dto.apikey.ApiKeyCreatedResponse;
import ec.tusaas.efactura.dto.apikey.ApiKeyResponse;
import ec.tusaas.efactura.entity.ApiKey;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.ApiKeyRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.TokenHasher;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final int PREFIX_LEN = 12;
  private static final int SECRET_BYTES = 24;

  private final ApiKeyRepository apiKeyRepository;
  private final EmpresaRepository empresaRepository;

  @Transactional(readOnly = true)
  public List<ApiKeyResponse> listar(UUID empresaId) {
    return apiKeyRepository.findByEmpresa_IdOrderByFechaCreacionDesc(empresaId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public ApiKeyCreatedResponse crear(UUID empresaId, ApiKeyCreateRequest request, String creadoPor) {
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    String prefix = generarPrefixUnico();
    byte[] secretBuf = new byte[SECRET_BYTES];
    RANDOM.nextBytes(secretBuf);
    String secret = HexFormat.of().formatHex(secretBuf);
    String plainKey = prefix + "." + secret;
    List<String> scopes = normalizarScopes(request.scopes());
    ApiKey k = new ApiKey();
    k.setEmpresa(empresa);
    k.setNombre(
        request.nombre() == null || request.nombre().isBlank() ? "API" : request.nombre().trim());
    k.setPrefix(prefix);
    k.setKeyHash(TokenHasher.sha256Hex(secret));
    k.setScopes(new ArrayList<>(scopes));
    k.setUsuarioCreacion(creadoPor);
    k = apiKeyRepository.save(k);
    return new ApiKeyCreatedResponse(
        k.getId(), k.getNombre(), k.getPrefix(), List.copyOf(k.getScopes()), plainKey);
  }

  /**
   * Valida cabecera {@code X-Api-Key} con formato {@code prefix.secret} y devuelve el principal de seguridad.
   */
  @Transactional
  public Optional<UsuarioPrincipal> autenticar(String rawHeader) {
    if (rawHeader == null || rawHeader.isBlank()) {
      return Optional.empty();
    }
    String raw = rawHeader.trim();
    int dot = raw.indexOf('.');
    if (dot <= 0 || dot == raw.length() - 1) {
      return Optional.empty();
    }
    String prefix = raw.substring(0, dot).trim();
    String secret = raw.substring(dot + 1).trim();
    if (prefix.isEmpty() || secret.isEmpty()) {
      return Optional.empty();
    }
    ApiKey k =
        apiKeyRepository
            .findByPrefixAndEstado(prefix, "ACTIVA")
            .orElse(null);
    if (k == null) {
      return Optional.empty();
    }
    if (k.getFechaExpiracion() != null && k.getFechaExpiracion().isBefore(Instant.now())) {
      return Optional.empty();
    }
    if (!TokenHasher.sha256Hex(secret).equals(k.getKeyHash())) {
      return Optional.empty();
    }
    k.setUltimoUso(Instant.now());
    UUID empresaId = k.getEmpresa().getId();
    List<String> scopes = k.getScopes() == null || k.getScopes().isEmpty() ? List.of("FACTURA_EMITIR") : k.getScopes();
    UsuarioPrincipal principal =
        UsuarioPrincipal.authenticatedWithApiKey(
            k.getId(), empresaId, "api-key:" + k.getPrefix(), scopes, k.getRateLimitRpm());
    return Optional.of(principal);
  }

  private ApiKeyResponse toResponse(ApiKey k) {
    return new ApiKeyResponse(
        k.getId(),
        k.getNombre(),
        k.getPrefix(),
        k.getScopes() == null ? List.of() : List.copyOf(k.getScopes()),
        k.getEstado(),
        k.getFechaExpiracion(),
        k.getUltimoUso(),
        k.getFechaCreacion());
  }

  private List<String> normalizarScopes(List<String> scopes) {
    if (scopes == null || scopes.isEmpty()) {
      return List.of("FACTURA_EMITIR");
    }
    List<String> out = new ArrayList<>();
    for (String s : scopes) {
      if (s == null || s.isBlank()) {
        continue;
      }
      String c = s.trim();
      if ("FACTURA_EMITIR".equals(c) || "REPORTE_VER".equals(c)) {
        if (!out.contains(c)) {
          out.add(c);
        }
      }
    }
    if (out.isEmpty()) {
      out.add("FACTURA_EMITIR");
    }
    return out;
  }

  @Transactional
  public void revocar(UUID empresaId, UUID keyId) {
    ApiKey k =
        apiKeyRepository
            .findByIdAndEmpresa_Id(keyId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key no encontrada"));
    k.setEstado("REVOCADA");
    apiKeyRepository.save(k);
  }

  private String generarPrefixUnico() {
    for (int i = 0; i < 20; i++) {
      String p = randomAlphanumeric(PREFIX_LEN);
      if (!apiKeyRepository.existsByPrefix(p)) {
        return p;
      }
    }
    throw new IllegalStateException("No se pudo generar prefix único para API key");
  }

  private static String randomAlphanumeric(int len) {
    char[] chars = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    StringBuilder sb = new StringBuilder(len);
    for (int i = 0; i < len; i++) {
      sb.append(chars[RANDOM.nextInt(chars.length)]);
    }
    return sb.toString();
  }
}
