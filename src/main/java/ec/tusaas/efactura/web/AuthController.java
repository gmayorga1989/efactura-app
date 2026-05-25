package ec.tusaas.efactura.web;

import ec.tusaas.efactura.dto.auth.ActivateTemporaryPasswordRequest;
import ec.tusaas.efactura.dto.auth.LoginRequest;
import ec.tusaas.efactura.dto.auth.LoginResult;
import ec.tusaas.efactura.dto.auth.LogoutRequest;
import ec.tusaas.efactura.dto.auth.PasswordResetConfirmRequest;
import ec.tusaas.efactura.dto.auth.PasswordResetRequest;
import ec.tusaas.efactura.dto.auth.RefreshRequest;
import ec.tusaas.efactura.dto.auth.SelectEmpresaRequest;
import ec.tusaas.efactura.dto.auth.SwitchEmpresaRequest;
import ec.tusaas.efactura.dto.auth.TokenResponse;
import ec.tusaas.efactura.dto.invitacion.AcceptInviteRequest;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.service.AuthService;
import ec.tusaas.efactura.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
    name = "Autenticación web",
    description =
        "Login multiempresa: paso 1 POST /login, paso 2 POST /select-empresa con sessionTicket. "
            + "JWT: subject = identidadId, claim empresaId = tenant actual (null = plataforma). "
            + "Permisos en claim authorities (códigos de tabla permiso). Ver docs/PERMISOS.md.")
@RestController
@RequestMapping("/api/web/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;
  private final PasswordResetService passwordResetService;

  @Operation(summary = "Login paso 1", description = "Devuelve tokens o SELECT_EMPRESA + sessionTicket + lista de empresas.")
  @PostMapping("/login")
  public LoginResult login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  @Operation(summary = "Login paso 2", description = "Canjea el sessionTicket por tokens de la empresa elegida.")
  @PostMapping("/select-empresa")
  public LoginResult selectEmpresa(@Valid @RequestBody SelectEmpresaRequest request) {
    return authService.selectEmpresa(request);
  }

  @Operation(summary = "Cambiar empresa", description = "Requiere Bearer access. Politica MFA/antigüedad vía efactura.jwt.switch-*")
  @PostMapping("/switch-empresa")
  @SecurityRequirement(name = "bearer-jwt")
  public TokenResponse switchEmpresa(
      @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
      @Valid @RequestBody SwitchEmpresaRequest request) {
    return authService.switchEmpresa(authorization, request);
  }

  @Operation(summary = "Aceptar invitación", description = "Público: crea identidad o exige contraseña existente y emite tokens.")
  @PostMapping("/accept-invite")
  public TokenResponse acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
    return authService.acceptInvite(request);
  }

  @PostMapping("/activate-temporary-password")
  public TokenResponse activateTemporaryPassword(
      @Valid @RequestBody ActivateTemporaryPasswordRequest request) {
    return authService.activateTemporaryPassword(request);
  }

  @Operation(
      summary = "Solicitar recuperacion de clave",
      description = "Publico: genera un token temporal y envia correo si el usuario activo existe.")
  @PostMapping("/password-reset/request")
  public void requestPasswordReset(
      @Valid @RequestBody PasswordResetRequest request, HttpServletRequest servletRequest) {
    passwordResetService.solicitar(
        request, servletRequest.getRemoteAddr(), servletRequest.getHeader("User-Agent"));
  }

  @Operation(
      summary = "Confirmar recuperacion de clave",
      description = "Publico: valida el token temporal, actualiza la clave y envia notificacion.")
  @PostMapping("/password-reset/confirm")
  public void confirmPasswordReset(@Valid @RequestBody PasswordResetConfirmRequest request) {
    passwordResetService.confirmar(request);
  }

  @Operation(
      summary = "Canje Identity Gateway",
      description =
          "Público: recibe Authorization Bearer con access token emitido por el Identity Gateway (iss/aud/secret "
              + "alineados con efactura.suite.identity.*) y devuelve tokens JWT de eFactura. Requiere scope "
              + "efactura.access o suite.admin y membresía activa cuyo empresa.id o empresa.suite_company_id "
              + "coincida con el claim company_id del token.")
  @PostMapping("/suite/exchange")
  public TokenResponse suiteExchange(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
    return authService.exchangeSuiteIdentityToken(authorization);
  }

  @Operation(summary = "Refrescar access", description = "Recalcula permisos desde BD para la misma sesión (identidad + empresa).")
  @PostMapping("/refresh")
  public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
    return authService.refresh(request);
  }

  @Operation(summary = "Cerrar sesión (refresh)", description = "Revoca el refresh token enviado, si corresponde al usuario autenticado.")
  @PostMapping("/logout")
  @SecurityRequirement(name = "bearer-jwt")
  public void logout(
      @AuthenticationPrincipal UsuarioPrincipal principal, @RequestBody(required = false) LogoutRequest body) {
    authService.logout(principal, body == null ? new LogoutRequest(null) : body);
  }
}
