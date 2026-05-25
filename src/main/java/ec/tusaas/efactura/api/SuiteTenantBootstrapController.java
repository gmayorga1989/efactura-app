package ec.tusaas.efactura.api;

import ec.tusaas.efactura.dto.auth.SuiteTenantBootstrapRequest;
import ec.tusaas.efactura.dto.auth.SuiteTenantBootstrapResultDto;
import ec.tusaas.efactura.service.suite.SuiteTenantProvisioningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Suite Identity público", description = "Aprovisionamiento tenant con JWT del Identity Gateway.")
@RestController
@RequestMapping("/api/public/v1/auth")
@RequiredArgsConstructor
public class SuiteTenantBootstrapController {

  private final SuiteTenantProvisioningService suiteTenantProvisioningService;

  @Operation(
      summary = "Aprovisionar empresa eFactura desde Identity",
      description =
          "Requiere Authorization Bearer con access token de Identity (misma config que suite/exchange). "
              + "Crea o enlaza empresa (suite_company_id), identidad con el email del token y membresía ADMIN si faltan.")
  @PostMapping("/suite/bootstrap")
  public SuiteTenantBootstrapResultDto suiteBootstrap(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @Valid @RequestBody SuiteTenantBootstrapRequest body) {
    return suiteTenantProvisioningService.bootstrapFromSuiteToken(authorization, body);
  }
}
