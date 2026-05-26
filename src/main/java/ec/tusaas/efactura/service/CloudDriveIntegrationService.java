package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.CloudDriveProperties;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudDriveIntegrationService {

  private static final String CONFIG_KEY = "cloudIntegrations";

  private final CloudDriveProperties properties;
  private final EmpresaRepository empresaRepository;
  private final RestClient restClient = RestClient.create();

  @Transactional(readOnly = true)
  public Map<String, Object> configPublico(UUID empresaId) {
    Map<String, Object> google = new HashMap<>();
    google.put("configured", properties.googleConfigured());
    google.put("clientId", properties.googleClientId());
    google.put("apiKey", properties.googleApiKey());
    google.put("connected", googleConnected(empresaId));

    Map<String, Object> microsoft = new HashMap<>();
    microsoft.put("configured", properties.microsoftConfigured());
    microsoft.put("clientId", properties.microsoftClientId());
    microsoft.put("connected", microsoftConnected(empresaId));

    return Map.of(
        "enabled", properties.enabled(),
        "google", google,
        "microsoft", microsoft);
  }

  public String googleAuthUrl(UUID empresaId) {
    requireGoogle();
    String state = encodeState(empresaId);
    String scope = urlEncode("https://www.googleapis.com/auth/drive.readonly");
    return "https://accounts.google.com/o/oauth2/v2/auth"
        + "?client_id="
        + urlEncode(properties.googleClientId())
        + "&redirect_uri="
        + urlEncode(properties.googleRedirectUri())
        + "&response_type=code&access_type=offline&prompt=consent&scope="
        + scope
        + "&state="
        + urlEncode(state);
  }

  public String microsoftAuthUrl(UUID empresaId) {
    requireMicrosoft();
    String state = encodeState(empresaId);
    String scope = urlEncode("offline_access Files.Read.All User.Read");
    String tenant = properties.microsoftTenant() == null ? "common" : properties.microsoftTenant();
    return "https://login.microsoftonline.com/"
        + tenant
        + "/oauth2/v2.0/authorize?client_id="
        + urlEncode(properties.microsoftClientId())
        + "&response_type=code&redirect_uri="
        + urlEncode(properties.microsoftRedirectUri())
        + "&response_mode=query&scope="
        + scope
        + "&state="
        + urlEncode(state);
  }

  @Transactional
  public String googleCallback(String code, String state) {
    UUID empresaId = decodeState(state);
    requireGoogle();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("code", code);
    form.add("client_id", properties.googleClientId());
    form.add("client_secret", properties.googleClientSecret());
    form.add("redirect_uri", properties.googleRedirectUri());
    form.add("grant_type", "authorization_code");
    @SuppressWarnings("unchecked")
    Map<String, Object> token =
        restClient
            .post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
    if (token == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Google no devolvió token");
    }
    guardarGoogleTokens(empresaId, token);
    return closeHtml("Google Drive conectado. Puede cerrar esta ventana.");
  }

  @Transactional
  public String microsoftCallback(String code, String state) {
    UUID empresaId = decodeState(state);
    requireMicrosoft();
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", properties.microsoftClientId());
    form.add("client_secret", properties.microsoftClientSecret());
    form.add("code", code);
    form.add("redirect_uri", properties.microsoftRedirectUri());
    form.add("grant_type", "authorization_code");
    String tenant = properties.microsoftTenant() == null ? "common" : properties.microsoftTenant();
    @SuppressWarnings("unchecked")
    Map<String, Object> token =
        restClient
            .post()
            .uri("https://login.microsoftonline.com/" + tenant + "/oauth2/v2.0/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
    if (token == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Microsoft no devolvió token");
    }
    guardarMicrosoftTokens(empresaId, token);
    return closeHtml("OneDrive conectado. Puede cerrar esta ventana.");
  }

  @Transactional(readOnly = true)
  public String googleAccessToken(UUID empresaId) {
    Map<String, Object> google = googleTokens(empresaId);
    if (google == null || google.get("accessToken") == null) {
      throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "Google Drive no conectado");
    }
    Instant exp = parseInstant(google.get("expiresAt"));
    if (exp != null && exp.isBefore(Instant.now().plusSeconds(60))) {
      refreshGoogle(empresaId, google);
      google = googleTokens(empresaId);
    }
    return String.valueOf(google.get("accessToken"));
  }

  private void refreshGoogle(UUID empresaId, Map<String, Object> google) {
    String refresh = String.valueOf(google.get("refreshToken"));
    if (refresh == null || refresh.isBlank() || "null".equals(refresh)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Reconecte Google Drive");
    }
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", properties.googleClientId());
    form.add("client_secret", properties.googleClientSecret());
    form.add("refresh_token", refresh);
    form.add("grant_type", "refresh_token");
    @SuppressWarnings("unchecked")
    Map<String, Object> token =
        restClient
            .post()
            .uri("https://oauth2.googleapis.com/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);
    if (token == null) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo renovar token Google");
    }
    if (token.get("refresh_token") == null) {
      token.put("refresh_token", refresh);
    }
    guardarGoogleTokens(empresaId, token);
  }

  private void guardarGoogleTokens(UUID empresaId, Map<String, Object> token) {
    Map<String, Object> row = new HashMap<>();
    row.put("accessToken", token.get("access_token"));
    row.put("refreshToken", token.get("refresh_token"));
    Object expiresIn = token.get("expires_in");
    if (expiresIn instanceof Number n) {
      row.put("expiresAt", Instant.now().plusSeconds(n.longValue()).toString());
    }
    row.put("updatedAt", Instant.now().toString());
    mergeCloudConfig(empresaId, "google", row);
  }

  private void guardarMicrosoftTokens(UUID empresaId, Map<String, Object> token) {
    Map<String, Object> row = new HashMap<>();
    row.put("accessToken", token.get("access_token"));
    row.put("refreshToken", token.get("refresh_token"));
    Object expiresIn = token.get("expires_in");
    if (expiresIn instanceof Number n) {
      row.put("expiresAt", Instant.now().plusSeconds(n.longValue()).toString());
    }
    row.put("updatedAt", Instant.now().toString());
    mergeCloudConfig(empresaId, "microsoft", row);
  }

  private void mergeCloudConfig(UUID empresaId, String provider, Map<String, Object> providerData) {
    Empresa e =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    Map<String, Object> extra = e.getConfigExtra() != null ? new HashMap<>(e.getConfigExtra()) : new HashMap<>();
    @SuppressWarnings("unchecked")
    Map<String, Object> cloud =
        extra.get(CONFIG_KEY) instanceof Map<?, ?> m
            ? new HashMap<>((Map<String, Object>) m)
            : new HashMap<>();
    cloud.put(provider, providerData);
    extra.put(CONFIG_KEY, cloud);
    e.setConfigExtra(extra);
    empresaRepository.save(e);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> googleTokens(UUID empresaId) {
    Empresa e = empresaRepository.findById(empresaId).orElse(null);
    if (e == null || e.getConfigExtra() == null) {
      return null;
    }
    Object cloud = e.getConfigExtra().get(CONFIG_KEY);
    if (!(cloud instanceof Map<?, ?> c)) {
      return null;
    }
    Object g = c.get("google");
    return g instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
  }

  private boolean googleConnected(UUID empresaId) {
    Map<String, Object> g = googleTokens(empresaId);
    return g != null && g.get("accessToken") != null;
  }

  private boolean microsoftConnected(UUID empresaId) {
    Empresa e = empresaRepository.findById(empresaId).orElse(null);
    if (e == null || e.getConfigExtra() == null) {
      return false;
    }
    Object cloud = e.getConfigExtra().get(CONFIG_KEY);
    if (!(cloud instanceof Map<?, ?> c)) {
      return false;
    }
    Object ms = c.get("microsoft");
    return ms instanceof Map<?, ?> m && m.get("accessToken") != null;
  }

  private static Instant parseInstant(Object raw) {
    if (raw == null) {
      return null;
    }
    try {
      return Instant.parse(String.valueOf(raw));
    } catch (Exception e) {
      return null;
    }
  }

  private static String encodeState(UUID empresaId) {
    return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(empresaId.toString().getBytes(StandardCharsets.UTF_8));
  }

  private static UUID decodeState(String state) {
    try {
      String decoded = new String(java.util.Base64.getUrlDecoder().decode(state), StandardCharsets.UTF_8);
      return UUID.fromString(decoded.trim());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "State OAuth inválido");
    }
  }

  private static String urlEncode(String v) {
    return URLEncoder.encode(v, StandardCharsets.UTF_8);
  }

  private static String closeHtml(String msg) {
    return "<!DOCTYPE html><html><body style='font-family:sans-serif;padding:2rem'><p>"
        + msg
        + "</p><script>window.close();</script></body></html>";
  }

  private void requireGoogle() {
    if (!properties.enabled() || !properties.googleConfigured()) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Integración Google no configurada");
    }
  }

  private void requireMicrosoft() {
    if (!properties.enabled() || !properties.microsoftConfigured()) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Integración Microsoft no configurada");
    }
  }
}
