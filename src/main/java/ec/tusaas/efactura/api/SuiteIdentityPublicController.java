package ec.tusaas.efactura.api;

import ec.tusaas.efactura.config.props.SuiteIdentityProperties;
import ec.tusaas.efactura.security.SuiteIdentityJwtService;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/v1/auth")
@RequiredArgsConstructor
public class SuiteIdentityPublicController {

  private final SuiteIdentityProperties suiteIdentityProperties;
  private final SuiteIdentityJwtService suiteIdentityJwtService;

  @GetMapping("/suite-identity")
  public Map<String, Object> suiteIdentityStatus() {
    Map<String, Object> m = new LinkedHashMap<>();
    boolean cryptoReady = suiteIdentityJwtService.cryptoParametersReady();
    boolean featureOn = suiteIdentityProperties.isEnabled();
    m.put("enabled", featureOn && cryptoReady);
    m.put("featureFlag", featureOn);
    m.put("cryptoReady", cryptoReady);
    String base = suiteIdentityProperties.getIdentityBaseUrl();
    if (base == null || base.isBlank()) {
      base = suiteIdentityProperties.getIssuer();
    }
    m.put("identityBaseUrl", base == null ? "" : base);
    m.put("issuer", suiteIdentityProperties.getIssuer() == null ? "" : suiteIdentityProperties.getIssuer());
    m.put(
        "companySlug",
        suiteIdentityProperties.getCompanySlug() == null ? "" : suiteIdentityProperties.getCompanySlug());
    m.put(
        "suiteShellBaseUrl",
        blankToEmpty(suiteIdentityProperties.getSuiteShellBaseUrl()));
    m.put("carteraBaseUrl", blankToEmpty(suiteIdentityProperties.getCarteraBaseUrl()));
    m.put("posBaseUrl", blankToEmpty(suiteIdentityProperties.getPosBaseUrl()));
    return m;
  }

  private static String blankToEmpty(String s) {
    return s == null || s.isBlank() ? "" : s.trim();
  }
}
