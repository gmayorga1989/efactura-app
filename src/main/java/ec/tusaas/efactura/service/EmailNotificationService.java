package ec.tusaas.efactura.service;

import ec.tusaas.efactura.config.props.NotificationProperties;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.entity.NotificacionEmail;
import ec.tusaas.efactura.repository.NotificacionEmailRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

  private static final String PROVIDER_BREVO = "BREVO";
  private static final DateTimeFormatter INVITATION_DATE_FORMAT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.of("America/Guayaquil"));

  private final NotificationProperties properties;
  private final NotificacionEmailRepository notificacionEmailRepository;
  private final RestClient.Builder restClientBuilder;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviarInvitacion(
      UUID empresaId,
      Empresa empresa,
      String email,
      String rolCodigo,
      String token,
      Instant expiresAt,
      String invitadoPor) {
    String acceptUrl = invitationUrl(token);
    String subject = "Invitacion a " + empresa.getRazonSocial();
    String html = invitacionHtml(empresa.getRazonSocial(), rolCodigo, acceptUrl, expiresAt, invitadoPor);
    String text = invitacionText(empresa.getRazonSocial(), rolCodigo, acceptUrl, expiresAt, invitadoPor);
    Map<String, Object> metadata =
        Map.of(
            "rolCodigo", rolCodigo,
            "acceptUrl", acceptUrl,
            "expiresAt", expiresAt.toString(),
            "invitadoPor", invitadoPor == null ? "" : invitadoPor);
    return enviar(empresaId, "INVITACION_USUARIO", email, null, subject, html, text, metadata);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviarUsuarioCreado(
      UUID empresaId,
      Empresa empresa,
      String email,
      String nombre,
      String rolCodigo,
      String passwordTemporal,
      String creadoPor) {
    String loginUrl = passwordTemporal == null || passwordTemporal.isBlank() ? loginUrl() : activationUrl(email, empresaId);
    String subject = "Tu usuario en " + empresa.getRazonSocial();
    String html = usuarioCreadoHtml(empresa.getRazonSocial(), nombre, rolCodigo, loginUrl, passwordTemporal, creadoPor);
    String text = usuarioCreadoText(empresa.getRazonSocial(), nombre, rolCodigo, loginUrl, passwordTemporal, creadoPor);
    Map<String, Object> metadata =
        Map.of(
            "rolCodigo", rolCodigo,
            "loginUrl", loginUrl,
            "requiereCambioPassword", passwordTemporal != null && !passwordTemporal.isBlank(),
            "creadoPor", creadoPor == null ? "" : creadoPor);
    return enviar(empresaId, "USUARIO_CREADO", email, nombre, subject, html, text, metadata);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviarRecuperacionPassword(String email, String nombre, String token, Instant expiresAt) {
    String resetUrl = passwordResetUrl(token);
    String subject = "Recuperacion de clave de acceso";
    String html = passwordResetHtml(nombre, resetUrl, expiresAt);
    String text = passwordResetText(nombre, resetUrl, expiresAt);
    Map<String, Object> metadata = Map.of("resetUrl", resetUrl, "expiresAt", expiresAt.toString());
    return enviar(null, "PASSWORD_RESET_SOLICITADO", email, nombre, subject, html, text, metadata);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviarPasswordActualizado(String email, String nombre) {
    String subject = "Tu clave de acceso fue actualizada";
    String html = passwordUpdatedHtml(nombre, loginUrl());
    String text = passwordUpdatedText(nombre, loginUrl());
    return enviar(null, "PASSWORD_RESET_CONFIRMADO", email, nombre, subject, html, text, Map.of("loginUrl", loginUrl()));
  }

  public record EmailAdjunto(String nombre, byte[] contenido) {}

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviarConAdjuntos(
      UUID empresaId,
      String tipo,
      String email,
      String nombre,
      String subject,
      String html,
      String text,
      List<EmailAdjunto> adjuntos,
      Map<String, Object> metadata) {
    return enviarInterno(empresaId, tipo, email, nombre, subject, html, text, adjuntos, metadata);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean enviar(
      UUID empresaId,
      String tipo,
      String email,
      String nombre,
      String subject,
      String html,
      String text,
      Map<String, Object> metadata) {
    return enviarInterno(empresaId, tipo, email, nombre, subject, html, text, List.of(), metadata);
  }

  private boolean enviarInterno(
      UUID empresaId,
      String tipo,
      String email,
      String nombre,
      String subject,
      String html,
      String text,
      List<EmailAdjunto> adjuntos,
      Map<String, Object> metadata) {
    String provider = normalizeProvider();
    NotificacionEmail row = new NotificacionEmail();
    row.setEmpresaId(empresaId);
    row.setTipo(tipo);
    row.setDestinatarioEmail(email);
    row.setDestinatarioNombre(nombre);
    row.setAsunto(subject);
    row.setProveedor(provider);
    row.setMetadata(metadata == null ? Map.of() : metadata);
    row = notificacionEmailRepository.saveAndFlush(row);

    if (!properties.isEnabled()) {
      row.setEstado("OMITIDO");
      row.setErrorMensaje("Notificaciones deshabilitadas");
      notificacionEmailRepository.save(row);
      log.info("[email] omitido tipo={} to={} subject={}", tipo, email, subject);
      return false;
    }

    if (!PROVIDER_BREVO.equals(provider)) {
      row.setEstado("OMITIDO");
      row.setErrorMensaje("Proveedor LOG");
      notificacionEmailRepository.save(row);
      log.info("[email] provider=LOG tipo={} to={} subject={}", tipo, email, subject);
      return false;
    }

    try {
      log.info(
          "[email] enviando provider=BREVO tipo={} to={} subject={} sender={} baseUrl={} apiKeyConfigured={}",
          tipo,
          email,
          subject,
          properties.getBrevo().getSenderEmail(),
          properties.getBrevo().getBaseUrl(),
          hasText(properties.getBrevo().getApiKey()));
      BrevoEmailResponse response = enviarBrevo(email, nombre, subject, html, text, adjuntos);
      row.setEstado("ENVIADO");
      row.setProviderMessageId(response == null ? null : response.messageId());
      row.setFechaEnvio(Instant.now());
      notificacionEmailRepository.save(row);
      log.info(
          "[email] enviado provider=BREVO tipo={} to={} notificationId={} messageId={}",
          tipo,
          email,
          row.getId(),
          response == null ? null : response.messageId());
      return true;
    } catch (RestClientResponseException e) {
      String detail = "HTTP " + e.getStatusCode() + " " + e.getResponseBodyAsString();
      row.setEstado("ERROR");
      row.setErrorMensaje(truncate(detail, 4000));
      notificacionEmailRepository.save(row);
      log.warn(
          "[email] brevo rechazo tipo={} to={} notificationId={} status={} response={}",
          tipo,
          email,
          row.getId(),
          e.getStatusCode(),
          e.getResponseBodyAsString());
      return false;
    } catch (Exception e) {
      row.setEstado("ERROR");
      row.setErrorMensaje(truncate(e.getMessage(), 4000));
      notificacionEmailRepository.save(row);
      log.warn("[email] error enviando tipo={} to={} notificationId={}: {}", tipo, email, row.getId(), e.getMessage(), e);
      return false;
    }
  }

  public String invitationUrl(String token) {
    String base = trimTrailingSlash(properties.getAppBaseUrl());
    return base + "/accept-invite?token=" + token;
  }

  public String loginUrl() {
    return trimTrailingSlash(properties.getAppBaseUrl()) + "/login";
  }

  public String activationUrl(String email, UUID empresaId) {
    String base = trimTrailingSlash(properties.getAppBaseUrl());
    StringBuilder url = new StringBuilder(base).append("/activate-temporary-password");
    url.append("?email=").append(urlEncode(email));
    if (empresaId != null) {
      url.append("&empresaId=").append(empresaId);
    }
    return url.toString();
  }

  public String passwordResetUrl(String token) {
    return trimTrailingSlash(properties.getAppBaseUrl()) + "/reset-password?token=" + urlEncode(token);
  }

  private BrevoEmailResponse enviarBrevo(
      String email, String nombre, String subject, String html, String text, List<EmailAdjunto> adjuntos) {
    var brevo = properties.getBrevo();
    if (brevo.getApiKey() == null || brevo.getApiKey().isBlank()) {
      throw new IllegalStateException("BREVO_API_KEY requerido para enviar correos");
    }
    List<BrevoAttachment> attachmentPayload = new ArrayList<>();
    if (adjuntos != null) {
      for (EmailAdjunto adj : adjuntos) {
        if (adj == null || adj.contenido() == null || adj.contenido().length == 0) {
          continue;
        }
        String name = adj.nombre() == null || adj.nombre().isBlank() ? "adjunto.bin" : adj.nombre();
        attachmentPayload.add(
            new BrevoAttachment(name, Base64.getEncoder().encodeToString(adj.contenido())));
      }
    }
    BrevoEmailRequest body =
        new BrevoEmailRequest(
            new EmailParty(brevo.getSenderEmail(), brevo.getSenderName()),
            List.of(new EmailParty(email, nombre)),
            subject,
            html,
            text,
            attachmentPayload.isEmpty() ? null : attachmentPayload);
    return restClientBuilder
        .baseUrl(trimTrailingSlash(brevo.getBaseUrl()))
        .build()
        .post()
        .uri("/v3/smtp/email")
        .contentType(MediaType.APPLICATION_JSON)
        .accept(MediaType.APPLICATION_JSON)
        .header("api-key", brevo.getApiKey())
        .body(body)
        .retrieve()
        .body(BrevoEmailResponse.class);
  }

  private String normalizeProvider() {
    if (properties.getProvider() == null || properties.getProvider().isBlank()) {
      return "LOG";
    }
    return properties.getProvider().trim().toUpperCase();
  }

  private static String trimTrailingSlash(String value) {
    if (value == null || value.isBlank()) {
      return "";
    }
    return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
  }

  private static boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private static String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }
    return value.substring(0, maxLength);
  }

  private static String urlEncode(String value) {
    return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
  }

  private static String invitacionHtml(
      String empresa, String rolCodigo, String acceptUrl, Instant expiresAt, String invitadoPorEmail) {
    String safeEmpresa = escapeHtml(empresa);
    String safeRol = escapeHtml(rolCodigo);
    String safeUrl = escapeHtml(acceptUrl);
    String safeInvitadoPor = escapeHtml(invitadoPorEmail == null ? "" : invitadoPorEmail);
    String safeExpiresAt = escapeHtml(INVITATION_DATE_FORMAT.format(expiresAt) + " (Ecuador)");
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fb;padding:32px 16px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                    <tr>
                      <td style="background:#0f766e;padding:24px 28px;color:#ffffff;">
                        <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">eFactura</div>
                        <div style="font-size:24px;font-weight:700;line-height:1.25;margin-top:8px;">Invitacion a %s</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:30px 28px 10px;">
                        <p style="font-size:16px;line-height:1.6;margin:0 0 18px;">Has sido invitado a formar parte de <strong>%s</strong>.</p>
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;margin:0 0 24px;">
                          <tr>
                            <td style="padding:16px 18px;">
                              <div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;">Rol asignado</div>
                              <div style="font-size:16px;font-weight:700;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 18px 16px;">
                              <div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;">Invitado por</div>
                              <div style="font-size:15px;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                        </table>
                        <div style="text-align:center;margin:28px 0 24px;">
                          <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 24px;border-radius:8px;">Aceptar invitacion</a>
                        </div>
                        <p style="font-size:14px;line-height:1.6;color:#64748b;margin:0 0 18px;">Este enlace expira el <strong style="color:#334155;">%s</strong>.</p>
                        <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0;">Si el boton no funciona, copia y pega este enlace en tu navegador:</p>
                        <p style="font-size:13px;line-height:1.6;word-break:break-all;margin:8px 0 0;"><a href="%s" style="color:#0f766e;">%s</a></p>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:22px 28px 26px;border-top:1px solid #eef2f7;color:#94a3b8;font-size:12px;line-height:1.5;">
                        Recibiste este correo porque un administrador te invito a una empresa en eFactura.
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """
        .formatted(safeEmpresa, safeEmpresa, safeRol, safeInvitadoPor, safeUrl, safeExpiresAt, safeUrl, safeUrl);
  }

  private static String invitacionText(
      String empresa, String rolCodigo, String acceptUrl, Instant expiresAt, String invitadoPorEmail) {
    return """
        Invitacion a %s

        Has sido invitado a formar parte de %s.
        Rol asignado: %s
        Invitado por: %s

        Acepta la invitacion aqui:
        %s

        Este enlace expira el %s (Ecuador).
        """
        .formatted(
            empresa,
            empresa,
            rolCodigo,
            invitadoPorEmail == null ? "" : invitadoPorEmail,
            acceptUrl,
            INVITATION_DATE_FORMAT.format(expiresAt));
  }

  private static String usuarioCreadoHtml(
      String empresa, String nombre, String rolCodigo, String loginUrl, String passwordTemporal, String creadoPor) {
    String safeEmpresa = escapeHtml(empresa);
    String safeNombre = escapeHtml(nombre == null || nombre.isBlank() ? "Usuario" : nombre);
    String safeRol = escapeHtml(rolCodigo);
    String safeLoginUrl = escapeHtml(loginUrl);
    String safePasswordTemporal = escapeHtml(passwordTemporal == null ? "" : passwordTemporal);
    String safeCreadoPor = escapeHtml(creadoPor == null ? "" : creadoPor);
    String passwordBlock =
        safePasswordTemporal.isBlank()
            ? "<p style=\"font-size:13px;line-height:1.6;color:#64748b;margin:0;\">Tu cuenta ya existia. Usa tu contrasena actual para ingresar.</p>"
            : """
              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#fff7ed;border:1px solid #fed7aa;border-radius:10px;margin:0 0 24px;">
                <tr>
                  <td style="padding:16px 18px;">
                    <div style="font-size:12px;color:#9a3412;text-transform:uppercase;letter-spacing:.04em;">Contrasena temporal</div>
                    <div style="font-size:20px;font-weight:700;letter-spacing:.04em;margin-top:6px;color:#7c2d12;">%s</div>
                    <div style="font-size:13px;line-height:1.5;color:#9a3412;margin-top:8px;">Debes cambiar esta contrasena para activar tu usuario.</div>
                  </td>
                </tr>
              </table>
              """
                .formatted(safePasswordTemporal);
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fb;padding:32px 16px;">
              <tr>
                <td align="center">
                  <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                    <tr>
                      <td style="background:#0f766e;padding:24px 28px;color:#ffffff;">
                        <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">eFactura</div>
                        <div style="font-size:24px;font-weight:700;line-height:1.25;margin-top:8px;">Tu usuario fue creado</div>
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:30px 28px 10px;">
                        <p style="font-size:16px;line-height:1.6;margin:0 0 18px;">Hola <strong>%s</strong>, ya tienes acceso a <strong>%s</strong>.</p>
                        <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border:1px solid #e5e7eb;border-radius:10px;margin:0 0 24px;">
                          <tr>
                            <td style="padding:16px 18px;">
                              <div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;">Rol asignado</div>
                              <div style="font-size:16px;font-weight:700;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                          <tr>
                            <td style="padding:0 18px 16px;">
                              <div style="font-size:12px;color:#64748b;text-transform:uppercase;letter-spacing:.04em;">Creado por</div>
                              <div style="font-size:15px;margin-top:4px;">%s</div>
                            </td>
                          </tr>
                        </table>
                        <div style="text-align:center;margin:28px 0 24px;">
                          <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 24px;border-radius:8px;">Ingresar a eFactura</a>
                        </div>
                        %s
                      </td>
                    </tr>
                    <tr>
                      <td style="padding:22px 28px 26px;border-top:1px solid #eef2f7;color:#94a3b8;font-size:12px;line-height:1.5;">
                        Si no esperabas este acceso, contacta al administrador de la empresa.
                      </td>
                    </tr>
                  </table>
                </td>
              </tr>
            </table>
          </body>
        </html>
        """
        .formatted(safeNombre, safeEmpresa, safeRol, safeCreadoPor, safeLoginUrl, passwordBlock);
  }

  private static String usuarioCreadoText(
      String empresa, String nombre, String rolCodigo, String loginUrl, String passwordTemporal, String creadoPor) {
    String passwordLine =
        passwordTemporal == null || passwordTemporal.isBlank()
            ? "Usa tu contrasena actual para ingresar."
            : "Contrasena temporal: " + passwordTemporal + "\nDebes cambiar esta contrasena para activar tu usuario.";
    return """
        Tu usuario fue creado

        Hola %s, ya tienes acceso a %s.
        Rol asignado: %s
        Creado por: %s

        Ingresa aqui:
        %s

        %s
        """
        .formatted(
            nombre == null || nombre.isBlank() ? "Usuario" : nombre,
            empresa,
            rolCodigo,
            creadoPor == null ? "" : creadoPor,
            loginUrl,
            passwordLine);
  }

  private static String passwordResetHtml(String nombre, String resetUrl, Instant expiresAt) {
    String safeNombre = escapeHtml(nombre == null || nombre.isBlank() ? "Usuario" : nombre);
    String safeUrl = escapeHtml(resetUrl);
    String safeExpiresAt = escapeHtml(INVITATION_DATE_FORMAT.format(expiresAt) + " (Ecuador)");
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fb;padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                  <tr><td style="background:#0f766e;padding:24px 28px;color:#ffffff;">
                    <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">eFactura</div>
                    <div style="font-size:24px;font-weight:700;line-height:1.25;margin-top:8px;">Recupera tu clave de acceso</div>
                  </td></tr>
                  <tr><td style="padding:30px 28px 10px;">
                    <p style="font-size:16px;line-height:1.6;margin:0 0 18px;">Hola <strong>%s</strong>, recibimos una solicitud para recuperar tu clave.</p>
                    <div style="text-align:center;margin:28px 0 24px;">
                      <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 24px;border-radius:8px;">Cambiar clave</a>
                    </div>
                    <p style="font-size:14px;line-height:1.6;color:#64748b;margin:0 0 18px;">Este enlace expira el <strong style="color:#334155;">%s</strong>.</p>
                    <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0;">Si no solicitaste este cambio, ignora este correo.</p>
                    <p style="font-size:13px;line-height:1.6;word-break:break-all;margin:12px 0 0;"><a href="%s" style="color:#0f766e;">%s</a></p>
                  </td></tr>
                  <tr><td style="padding:22px 28px 26px;border-top:1px solid #eef2f7;color:#94a3b8;font-size:12px;line-height:1.5;">
                    Por seguridad, este enlace solo puede usarse una vez.
                  </td></tr>
                </table>
              </td></tr>
            </table>
          </body>
        </html>
        """
        .formatted(safeNombre, safeUrl, safeExpiresAt, safeUrl, safeUrl);
  }

  private static String passwordResetText(String nombre, String resetUrl, Instant expiresAt) {
    return """
        Recupera tu clave de acceso

        Hola %s, recibimos una solicitud para recuperar tu clave.

        Cambia tu clave aqui:
        %s

        Este enlace expira el %s (Ecuador).
        Si no solicitaste este cambio, ignora este correo.
        """
        .formatted(
            nombre == null || nombre.isBlank() ? "Usuario" : nombre,
            resetUrl,
            INVITATION_DATE_FORMAT.format(expiresAt));
  }

  private static String passwordUpdatedHtml(String nombre, String loginUrl) {
    String safeNombre = escapeHtml(nombre == null || nombre.isBlank() ? "Usuario" : nombre);
    String safeUrl = escapeHtml(loginUrl);
    return """
        <!doctype html>
        <html lang="es">
          <body style="margin:0;padding:0;background:#f5f7fb;font-family:Arial,Helvetica,sans-serif;color:#1f2937;">
            <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f5f7fb;padding:32px 16px;">
              <tr><td align="center">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="max-width:620px;background:#ffffff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;">
                  <tr><td style="background:#0f766e;padding:24px 28px;color:#ffffff;">
                    <div style="font-size:13px;letter-spacing:.08em;text-transform:uppercase;opacity:.85;">eFactura</div>
                    <div style="font-size:24px;font-weight:700;line-height:1.25;margin-top:8px;">Clave actualizada</div>
                  </td></tr>
                  <tr><td style="padding:30px 28px 10px;">
                    <p style="font-size:16px;line-height:1.6;margin:0 0 18px;">Hola <strong>%s</strong>, tu clave de acceso fue actualizada correctamente.</p>
                    <div style="text-align:center;margin:28px 0 24px;">
                      <a href="%s" style="display:inline-block;background:#0f766e;color:#ffffff;text-decoration:none;font-size:16px;font-weight:700;padding:14px 24px;border-radius:8px;">Ir al login</a>
                    </div>
                    <p style="font-size:13px;line-height:1.6;color:#64748b;margin:0;">Si no realizaste este cambio, contacta inmediatamente al administrador.</p>
                  </td></tr>
                </table>
              </td></tr>
            </table>
          </body>
        </html>
        """
        .formatted(safeNombre, safeUrl);
  }

  private static String passwordUpdatedText(String nombre, String loginUrl) {
    return """
        Clave actualizada

        Hola %s, tu clave de acceso fue actualizada correctamente.

        Login:
        %s

        Si no realizaste este cambio, contacta inmediatamente al administrador.
        """
        .formatted(nombre == null || nombre.isBlank() ? "Usuario" : nombre, loginUrl);
  }

  private static String escapeHtml(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;");
  }

  private record EmailParty(String email, String name) {}

  private record BrevoAttachment(String name, String content) {}

  private record BrevoEmailRequest(
      EmailParty sender,
      List<EmailParty> to,
      String subject,
      String htmlContent,
      String textContent,
      List<BrevoAttachment> attachment) {}

  private record BrevoEmailResponse(String messageId) {}
}
