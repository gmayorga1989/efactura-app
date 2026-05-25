package ec.tusaas.efactura.service;

import ec.tusaas.efactura.emision.EstadoSri;
import ec.tusaas.efactura.emision.Sha256;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteArchivo;
import ec.tusaas.efactura.entity.ComprobanteLogSri;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.sri.ComprobanteSriReemisionSupport;
import ec.tusaas.efactura.sri.client.SriAutorizacionClient;
import ec.tusaas.efactura.sri.client.SriRecepcionClient;
import ec.tusaas.efactura.sri.signature.SignedXml;
import ec.tusaas.efactura.sri.signature.XmlSignerService;
import ec.tusaas.efactura.storage.LocalComprobanteStorage;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteSriProcesoService {

  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteArchivoRepository comprobanteArchivoRepository;
  private final ComprobanteLogSriRepository comprobanteLogSriRepository;
  private final XmlSignerService xmlSignerService;
  private final SriRecepcionClient sriRecepcionClient;
  private final SriAutorizacionClient sriAutorizacionClient;
  private final LocalComprobanteStorage localComprobanteStorage;
  private final ComprobanteRideService comprobanteRideService;
  private final ComprobanteNotificacionService comprobanteNotificacionService;

  public void ejecutar(
      Comprobante c, Empresa empresa, Certificado certificado, String xmlGenerado, Consumer<String> validarXsd) {
    if (validarXsd != null) {
      try {
        validarXsd.accept(xmlGenerado);
      } catch (IllegalStateException e) {
        throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage(), e);
      }
    }
    c.setEstadoSri(EstadoSri.FIRMADO);
    comprobanteRepository.save(c);
    SignedXml xmlFirmado = xmlSignerService.firmar(xmlGenerado, certificado);
    registrarArchivo(c, "XML_GENERADO", xmlGenerado);
    registrarArchivo(c, xmlFirmado.stub() ? "XML_FIRMADO_STUB" : "XML_FIRMADO", xmlFirmado.xml());
    registrarLogSri(
        c,
        empresa,
        "FIRMA",
        "claveAcceso=" + c.getClaveAcceso(),
        xmlFirmado.stub() ? "Firma en modo stub (desarrollo)" : "XML firmado correctamente",
        200,
        xmlFirmado.stub() ? "Firma local (stub)" : "Documento firmado");
    log.info(
        "SRI recepción: clave={} ambiente={} tipo={}",
        c.getClaveAcceso(),
        c.getAmbienteSri(),
        c.getTipo());
    var recepcion = sriRecepcionClient.enviar(xmlFirmado.xml(), c.getAmbienteSri());
    c.setEstadoSri(normalizarEstadoRecepcion(recepcion.estado()));
    comprobanteRepository.save(c);
    log.info(
        "SRI recepción respuesta: clave={} estado={} http={} mensaje={}",
        c.getClaveAcceso(),
        recepcion.estado(),
        recepcion.httpStatus(),
        recepcion.mensaje());
    if (log.isDebugEnabled() && recepcion.rawResponse() != null) {
      log.debug("SRI recepción raw ({} chars): {}", recepcion.rawResponse().length(), recepcion.rawResponse());
    }
    registrarLogSri(
        c, empresa, "RECEPCION", "claveAcceso=" + c.getClaveAcceso(), recepcion.rawResponse(),
        recepcion.httpStatus(), recepcion.mensaje());

    if ("RECIBIDA".equalsIgnoreCase(recepcion.estado())) {
      log.info("SRI autorización: clave={} ambiente={}", c.getClaveAcceso(), c.getAmbienteSri());
      var autorizacion = sriAutorizacionClient.consultar(c.getClaveAcceso(), c.getAmbienteSri());
      c.setEstadoSri(normalizarEstadoAutorizacion(autorizacion.estado()));
      c.setNumeroAutorizacion(autorizacion.numeroAutorizacion());
      c.setFechaAutorizacion(autorizacion.fechaAutorizacion());
      comprobanteRepository.save(c);
      if ("AUTORIZADO".equalsIgnoreCase(c.getEstadoSri()) && autorizacion.rawResponse() != null) {
        registrarArchivo(c, "XML_AUTORIZADO", autorizacion.rawResponse());
      }
      log.info(
          "SRI autorización respuesta: clave={} estado={} nro={} mensaje={}",
          c.getClaveAcceso(),
          c.getEstadoSri(),
          c.getNumeroAutorizacion(),
          autorizacion.mensaje());
      registrarLogSri(
          c, empresa, "AUTORIZACION", "claveAcceso=" + c.getClaveAcceso(), autorizacion.rawResponse(), 200,
          autorizacion.mensaje());
      notificarClienteSiAutorizado(c);
    } else {
      String estadoRec = normalizarEstadoRecepcion(recepcion.estado());
      if (EstadoSri.ERROR.equalsIgnoreCase(estadoRec) || EstadoSri.DEVUELTO.equalsIgnoreCase(estadoRec)) {
      log.warn(
          "SRI recepción no exitosa: clave={} estado={} mensaje={}",
          c.getClaveAcceso(),
          recepcion.estado(),
          recepcion.mensaje());
      }
    }
  }

  public void reconsultarAutorizacion(Comprobante c, Empresa empresa) {
    String estado = c.getEstadoSri() == null ? "" : c.getEstadoSri().trim().toUpperCase();
    if (!ComprobanteSriReemisionSupport.puedeReconsultarAutorizacion(estado)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "El comprobante no está pendiente de autorización. Use reenvío al SRI si fue devuelto o rechazado.");
    }
    log.info("SRI reconsulta autorización: clave={} estadoActual={}", c.getClaveAcceso(), c.getEstadoSri());
    var autorizacion = sriAutorizacionClient.consultar(c.getClaveAcceso(), c.getAmbienteSri());
    c.setEstadoSri(normalizarEstadoAutorizacion(autorizacion.estado()));
    c.setNumeroAutorizacion(autorizacion.numeroAutorizacion());
    c.setFechaAutorizacion(autorizacion.fechaAutorizacion());
    comprobanteRepository.save(c);
    if ("AUTORIZADO".equalsIgnoreCase(c.getEstadoSri()) && autorizacion.rawResponse() != null) {
      registrarArchivo(c, "XML_AUTORIZADO", autorizacion.rawResponse());
    }
    registrarLogSri(
        c, empresa, "AUTORIZACION_REINTENTO", "claveAcceso=" + c.getClaveAcceso(), autorizacion.rawResponse(), 200,
        autorizacion.mensaje());
    asegurarRide(c);
    notificarClienteSiAutorizado(c);
  }

  /** Envía RIDE/XML al correo del receptor tras autorización SRI (no bloquea la emisión si falla). */
  public void notificarClienteSiAutorizado(Comprobante c) {
    if (c == null || !"AUTORIZADO".equalsIgnoreCase(c.getEstadoSri())) {
      return;
    }
    String email = ComprobanteNotificacionService.resolverEmailReceptor(c);
    if (email == null || email.isBlank()) {
      log.info("Correo automático omitido: comprobanteId={} sin email receptor", c.getId());
      return;
    }
    try {
      asegurarRide(c);
      boolean enviado = comprobanteNotificacionService.enviarComprobanteCliente(c);
      log.info("Correo automático comprobanteId={} enviado={}", c.getId(), enviado);
    } catch (Exception e) {
      log.warn("No se pudo enviar correo automático comprobanteId={}: {}", c.getId(), e.getMessage());
    }
  }

  public void asegurarRide(Comprobante c) {
    if ("AUTORIZADO".equalsIgnoreCase(c.getEstadoSri())) {
      registrarRide(c);
      return;
    }
    comprobanteArchivoRepository
        .findFirstByComprobante_IdAndTipoOrderByFechaCreacionDesc(c.getId(), "RIDE_PDF")
        .orElseGet(() -> registrarRide(c));
  }

  private ComprobanteArchivo registrarRide(Comprobante c) {
    byte[] pdf = comprobanteRideService.generarPdf(c);
    ComprobanteArchivo a = new ComprobanteArchivo();
    a.setComprobante(c);
    a.setTipo("RIDE_PDF");
    a.setSha256(Sha256.hex(pdf));
    a.setTamanoBytes((long) pdf.length);
    try {
      a.setStorageKey(localComprobanteStorage.guardarBytes(c.getEmpresa().getId(), c.getId(), "RIDE_PDF", "pdf", pdf));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el RIDE");
    }
    return comprobanteArchivoRepository.save(a);
  }

  private void registrarArchivo(Comprobante c, String tipo, String contenido) {
    ComprobanteArchivo a = new ComprobanteArchivo();
    a.setComprobante(c);
    a.setTipo(tipo);
    a.setSha256(Sha256.hex(contenido));
    a.setTamanoBytes((long) contenido.getBytes(StandardCharsets.UTF_8).length);
    try {
      a.setStorageKey(localComprobanteStorage.guardarTexto(c.getEmpresa().getId(), c.getId(), tipo, contenido));
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo almacenar el XML");
    }
    comprobanteArchivoRepository.save(a);
  }

  private void registrarLogSri(
      Comprobante c,
      Empresa empresa,
      String operacion,
      String request,
      String response,
      int httpStatus,
      String mensaje) {
    ComprobanteLogSri log = new ComprobanteLogSri();
    log.setComprobante(c);
    log.setEmpresa(empresa);
    log.setOperacion(operacion);
    log.setRequest(request);
    log.setResponse(response);
    log.setHttpStatus(httpStatus);
    log.setDuracionMs(0);
    log.setErrorMensaje(mensaje);
    log.setFecha(Instant.now());
    comprobanteLogSriRepository.save(log);
  }

  private static String normalizarEstadoAutorizacion(String estado) {
    if (estado == null || estado.isBlank()) {
      return EstadoSri.PENDIENTE_AUTORIZACION;
    }
    return estado;
  }

  private static String normalizarEstadoRecepcion(String estado) {
    if (estado == null || estado.isBlank()) {
      return EstadoSri.ERROR;
    }
    if ("DEVUELTA".equalsIgnoreCase(estado.trim())) {
      return EstadoSri.DEVUELTO;
    }
    return estado.trim();
  }
}
