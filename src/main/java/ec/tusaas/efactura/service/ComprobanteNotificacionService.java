package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.email.EmailPlantillaDto;
import ec.tusaas.efactura.emision.email.ComprobanteEmailRenderer;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteArchivo;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.storage.LocalComprobanteStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteNotificacionService {

  private final EmailNotificationService emailNotificationService;
  private final EmailPlantillaService emailPlantillaService;
  private final ComprobanteArchivoRepository comprobanteArchivoRepository;
  private final LocalComprobanteStorage localComprobanteStorage;
  private final ComprobanteRideService comprobanteRideService;

  public boolean enviarComprobanteCliente(Comprobante comprobante) {
    validarComprobanteAutorizado(comprobante);
    Empresa empresa = comprobante.getEmpresa();
    String email = resolverEmailReceptor(comprobante);
    if (email == null || email.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comprobante no tiene correo del receptor");
    }
    String numero = numeroComprobante(comprobante);
    EmailPlantillaDto plantilla = emailPlantillaService.desdeEmpresa(empresa);
    String asunto = ComprobanteEmailRenderer.renderAsunto(plantilla, comprobante, numero);
    String logoUrl = resolverLogoPublico(empresa);
    String html = ComprobanteEmailRenderer.renderHtml(plantilla, empresa, comprobante, numero, logoUrl);
    String text = ComprobanteEmailRenderer.renderText(plantilla, empresa, comprobante, numero);
    List<EmailNotificationService.EmailAdjunto> adjuntos = cargarAdjuntos(comprobante, numero);
    boolean tieneXml =
        adjuntos.stream().anyMatch(a -> a.nombre() != null && a.nombre().toLowerCase().endsWith(".xml"));
    if (!tieneXml) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "No existe XML autorizado del SRI para adjuntar al correo. Reconsulte la autorización.");
    }
    if (adjuntos.isEmpty()) {
      log.warn("Enviando correo sin adjuntos comprobanteId={}", comprobante.getId());
    }
    return emailNotificationService.enviarConAdjuntos(
        empresa.getId(),
        "COMPROBANTE_CLIENTE",
        email,
        comprobante.getRazonSocialReceptor(),
        asunto,
        html,
        text,
        adjuntos,
        Map.of("comprobanteId", comprobante.getId(), "numero", numero));
  }

  private List<EmailNotificationService.EmailAdjunto> cargarAdjuntos(Comprobante c, String numero) {
    List<EmailNotificationService.EmailAdjunto> out = new ArrayList<>();
    byte[] pdf = cargarRidePdf(c);
    if (pdf != null && pdf.length > 0) {
      out.add(new EmailNotificationService.EmailAdjunto("RIDE-" + numero + ".pdf", pdf));
    }
    byte[] xml = cargarXml(c);
    if (xml != null && xml.length > 0) {
      out.add(new EmailNotificationService.EmailAdjunto("XML-" + numero + ".xml", xml));
    }
    return out;
  }

  private byte[] cargarRidePdf(Comprobante c) {
    try {
      return comprobanteRideService.generarPdf(c);
    } catch (Exception e) {
      log.warn("No se pudo generar RIDE para correo comprobanteId={}: {}", c.getId(), e.getMessage());
      return null;
    }
  }

  private byte[] cargarXml(Comprobante c) {
    var opt =
        comprobanteArchivoRepository.findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(
            c.getId(), "XML_AUTORIZADO");
    if (opt.isEmpty()) {
      log.warn("Correo sin XML_AUTORIZADO comprobanteId={}", c.getId());
      return null;
    }
    try {
      return leerArchivoSeguro(opt.get());
    } catch (Exception e) {
      log.warn("No se leyó XML_AUTORIZADO comprobanteId={}: {}", c.getId(), e.getMessage());
      return null;
    }
  }

  private static void validarComprobanteAutorizado(Comprobante c) {
    if (c == null || !"AUTORIZADO".equalsIgnoreCase(c.getEstadoSri())) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Solo se puede enviar el correo cuando el comprobante está autorizado por el SRI");
    }
    if (c.getNumeroAutorizacion() == null || c.getNumeroAutorizacion().isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El comprobante no tiene número de autorización del SRI");
    }
  }

  private byte[] leerArchivoSeguro(ComprobanteArchivo archivo) {
    try {
      return localComprobanteStorage.leerBytes(archivo.getStorageKey());
    } catch (IOException e) {
      log.debug("No se leyo archivo {}: {}", archivo.getTipo(), e.getMessage());
      return null;
    }
  }

  static String resolverEmailReceptor(Comprobante c) {
    Map<String, Object> cd = c.getCustomData();
    if (cd == null) {
      return null;
    }
    Object email = cd.get("emailReceptor");
    if (email != null && !String.valueOf(email).isBlank()) {
      return String.valueOf(email).trim();
    }
    Object emails = cd.get("emailsReceptor");
    if (emails instanceof List<?> list && !list.isEmpty()) {
      Object first = list.get(0);
      if (first != null && !String.valueOf(first).isBlank()) {
        return String.valueOf(first).trim();
      }
    }
    return null;
  }

  static String numeroComprobante(Comprobante c) {
    return c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial();
  }

  private static String resolverLogoPublico(Empresa empresa) {
    String url = empresa.getLogoUrl();
    if (url == null || url.isBlank()) {
      return null;
    }
    url = url.trim();
    if (url.startsWith("http://") || url.startsWith("https://")) {
      return url;
    }
    return null;
  }
}
