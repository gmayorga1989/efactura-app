package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.comprobante.HistorialElectronicoEventoDto;
import ec.tusaas.efactura.dto.comprobante.SriMensajeHistorialDto;
import ec.tusaas.efactura.sri.client.SriRespuestaHistorialParser;
import ec.tusaas.efactura.entity.Auditoria;
import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.ComprobanteArchivo;
import ec.tusaas.efactura.entity.ComprobanteLogSri;
import ec.tusaas.efactura.entity.NotificacionEmail;
import ec.tusaas.efactura.repository.AuditoriaRepository;
import ec.tusaas.efactura.repository.ComprobanteArchivoRepository;
import ec.tusaas.efactura.repository.ComprobanteLogSriRepository;
import ec.tusaas.efactura.repository.ComprobanteRepository;
import ec.tusaas.efactura.repository.NotificacionEmailRepository;
import java.time.LocalDate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ComprobanteHistorialElectronicoService {

  private static final ObjectMapper JSON = new ObjectMapper();

  private static final String ENTIDAD_COMPROBANTE = "Comprobante";
  private static final String TIPO_CORREO_COMPROBANTE = "COMPROBANTE_CLIENTE";

  private final ComprobanteRepository comprobanteRepository;
  private final ComprobanteLogSriRepository comprobanteLogSriRepository;
  private final ComprobanteArchivoRepository comprobanteArchivoRepository;
  private final AuditoriaRepository auditoriaRepository;
  private final NotificacionEmailRepository notificacionEmailRepository;

  @Transactional(readOnly = true)
  public List<HistorialElectronicoEventoDto> historialComprobante(UUID empresaId, UUID comprobanteId) {
    Comprobante c = cargarComprobante(empresaId, comprobanteId);
    String numero = numero(c);
    List<HistorialElectronicoEventoDto> eventos = new ArrayList<>();

    if (c.getFechaCreacion() != null) {
      eventos.add(
          new HistorialElectronicoEventoDto(
              c.getId(),
              c.getFechaCreacion(),
              "EMISION",
              "CREACION",
              "Comprobante registrado",
              "Estado inicial: " + safe(c.getEstadoSri()),
              "INFO",
              null,
              c.getId(),
              numero,
              c.getTipo()));
    }

    for (Auditoria a : auditoriaRepository.findByEmpresa_IdAndEntidadAndEntidadIdOrderByFechaAsc(
        empresaId, ENTIDAD_COMPROBANTE, comprobanteId)) {
      eventos.add(desdeAuditoria(a, c, numero));
    }

    for (ComprobanteArchivo archivo :
        comprobanteArchivoRepository.findByComprobante_IdOrderByFechaCreacionAsc(comprobanteId)) {
      eventos.add(desdeArchivo(archivo, c, numero));
    }

    for (ComprobanteLogSri log : comprobanteLogSriRepository.findByComprobante_IdOrderByFechaAsc(comprobanteId)) {
      eventos.add(desdeLogSri(log, c, numero));
    }

    for (NotificacionEmail mail :
        notificacionEmailRepository.findByEmpresaIdAndTipoOrderByFechaCreacionAsc(empresaId, TIPO_CORREO_COMPROBANTE)) {
      if (perteneceComprobante(mail, comprobanteId)) {
        eventos.add(desdeCorreo(mail, c, numero));
      }
    }

    eventos.sort(Comparator.comparing(HistorialElectronicoEventoDto::fecha).reversed());
    return List.copyOf(eventos);
  }

  @Transactional(readOnly = true)
  public Page<HistorialElectronicoEventoDto> historialEmpresa(
      UUID empresaId, LocalDate fechaDesde, LocalDate fechaHasta, Pageable pageable) {
    return comprobanteLogSriRepository
        .findHistorialEmpresa(empresaId, fechaDesde, fechaHasta, pageable)
        .map(log -> desdeLogSri(log, log.getComprobante(), numero(log.getComprobante())));
  }

  private Comprobante cargarComprobante(UUID empresaId, UUID comprobanteId) {
    return comprobanteRepository
        .findByIdAndEmpresa_Id(comprobanteId, empresaId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comprobante no encontrado"));
  }

  private static HistorialElectronicoEventoDto desdeLogSri(ComprobanteLogSri log, Comprobante c, String numero) {
    String op = safe(log.getOperacion());
    String titulo = tituloOperacionSri(op);
    List<SriMensajeHistorialDto> mensajesSri = SriRespuestaHistorialParser.mensajes(log.getResponse());
    String mensaje = SriRespuestaHistorialParser.resumenLegible(log.getResponse(), log.getErrorMensaje());
    if (mensaje.isBlank()) {
      mensaje = "Sin mensaje adicional";
    }
    String resultado = resultadoLogSri(log.getHttpStatus(), mensaje, log.getResponse());
    String solicitud = log.getRequest() == null ? "" : log.getRequest().trim();
    String respuesta =
        log.getResponse() == null ? "" : SriRespuestaHistorialParser.formatearXml(log.getResponse());
    return new HistorialElectronicoEventoDto(
        log.getId(),
        log.getFecha(),
        "SRI",
        op,
        titulo,
        mensaje,
        resultado,
        log.getHttpStatus(),
        c.getId(),
        numero,
        c.getTipo(),
        true,
        solicitud,
        respuesta,
        mensajesSri);
  }

  private static HistorialElectronicoEventoDto desdeArchivo(
      ComprobanteArchivo archivo, Comprobante c, String numero) {
    String tipo = safe(archivo.getTipo());
    String detalle =
        "Tipo: "
            + tipo
            + "\nTamaño: "
            + (archivo.getTamanoBytes() == null ? "—" : archivo.getTamanoBytes() + " bytes")
            + "\nSHA-256: "
            + safe(archivo.getSha256())
            + "\nAlmacenamiento: "
            + safe(archivo.getStorageKey());
    return new HistorialElectronicoEventoDto(
        archivo.getId(),
        archivo.getFechaCreacion(),
        "ARCHIVO",
        tipo,
        tituloArchivo(tipo),
        archivo.getTamanoBytes() == null ? "" : "Tamaño: " + archivo.getTamanoBytes() + " bytes",
        "OK",
        null,
        c.getId(),
        numero,
        c.getTipo(),
        true,
        null,
        detalle,
        List.of());
  }

  private static HistorialElectronicoEventoDto desdeAuditoria(Auditoria a, Comprobante c, String numero) {
    String accion = safe(a.getAccion());
    String detalle = formatearJson(a.getCambios());
    return new HistorialElectronicoEventoDto(
        a.getId(),
        a.getFecha(),
        "AUDITORIA",
        accion,
        tituloAuditoria(accion),
        resumenCambios(a.getCambios()),
        "INFO",
        null,
        c.getId(),
        numero,
        c.getTipo(),
        detalle != null && !detalle.isBlank(),
        safe(a.getUsuario()),
        detalle,
        List.of());
  }

  private static HistorialElectronicoEventoDto desdeCorreo(
      NotificacionEmail mail, Comprobante c, String numero) {
    String estado = safe(mail.getEstado());
    String resultado = "ENVIADO".equalsIgnoreCase(estado) ? "OK" : "ERROR";
    String mensaje =
        mail.getErrorMensaje() != null && !mail.getErrorMensaje().isBlank()
            ? mail.getErrorMensaje()
            : "Destinatario: " + mail.getDestinatarioEmail() + " · " + mail.getAsunto();
    String detalle =
        "Estado: "
            + estado
            + "\nDestinatario: "
            + safe(mail.getDestinatarioEmail())
            + "\nAsunto: "
            + safe(mail.getAsunto())
            + (mail.getErrorMensaje() != null && !mail.getErrorMensaje().isBlank()
                ? "\nError: " + mail.getErrorMensaje()
                : "");
    return new HistorialElectronicoEventoDto(
        mail.getId(),
        mail.getFechaCreacion(),
        "CORREO",
        estado,
        "Correo al cliente",
        mensaje,
        resultado,
        null,
        c.getId(),
        numero,
        c.getTipo(),
        true,
        null,
        detalle,
        List.of());
  }

  private static boolean perteneceComprobante(NotificacionEmail mail, UUID comprobanteId) {
    Map<String, Object> meta = mail.getMetadata();
    if (meta == null) {
      return false;
    }
    Object raw = meta.get("comprobanteId");
    return raw != null && comprobanteId.toString().equalsIgnoreCase(String.valueOf(raw));
  }

  private static String numero(Comprobante c) {
    return c.getEstablecimientoCodigo() + "-" + c.getPuntoEmisionCodigo() + "-" + c.getSecuencial();
  }

  private static String tituloOperacionSri(String op) {
    return switch (op.toUpperCase()) {
      case "FIRMA" -> "Firma electrónica del XML";
      case "RECEPCION" -> "Envío al SRI (recepción)";
      case "AUTORIZACION" -> "Consulta de autorización SRI";
      case "AUTORIZACION_REINTENTO" -> "Reconsulta de autorización SRI";
      default -> "Proceso SRI: " + op;
    };
  }

  private static String tituloArchivo(String tipo) {
    return switch (tipo.toUpperCase()) {
      case "XML_GENERADO" -> "XML del comprobante generado";
      case "XML_FIRMADO", "XML_FIRMADO_STUB" -> "XML firmado electrónicamente";
      case "XML_AUTORIZADO" -> "XML autorizado por el SRI";
      case "RIDE_PDF" -> "RIDE (PDF) generado";
      default -> "Archivo: " + tipo;
    };
  }

  private static String tituloAuditoria(String accion) {
    if (accion.contains("EMITIDA")) {
      return "Emisión del comprobante";
    }
    if (accion.contains("BORRADOR")) {
      return "Borrador guardado";
    }
    return accion.replace('_', ' ');
  }

  private static String resumenCambios(Map<String, Object> cambios) {
    if (cambios == null || cambios.isEmpty()) {
      return "";
    }
    Object estado = cambios.get("estadoSri");
    if (estado != null) {
      return "Estado SRI: " + estado;
    }
    return cambios.toString();
  }

  private static String formatearJson(Map<String, Object> cambios) {
    if (cambios == null || cambios.isEmpty()) {
      return "";
    }
    try {
      return JSON.writerWithDefaultPrettyPrinter().writeValueAsString(cambios);
    } catch (JsonProcessingException e) {
      return cambios.toString();
    }
  }

  private static String resultadoLogSri(Integer httpStatus, String mensaje, String response) {
    if (esRespuestaStub(response)) {
      return "WARN";
    }
    return resultadoHttp(httpStatus, mensaje);
  }

  private static boolean esRespuestaStub(String response) {
    return response != null && response.contains("_STUB");
  }

  private static String resultadoHttp(Integer httpStatus, String mensaje) {
    if (httpStatus != null && httpStatus >= 400) {
      return "ERROR";
    }
    if (mensaje != null) {
      String m = mensaje.toLowerCase();
      if (m.contains("desactivada")
          || m.contains("no configurado")
          || m.contains("soap sri")
          || m.contains("stub")) {
        return "WARN";
      }
      if (m.contains("error")
          || m.contains("rechaz")
          || m.contains("devuelt")
          || m.contains("no cumple")
          || m.contains("no autorizado")) {
        return "ERROR";
      }
    }
    return "OK";
  }

  private static String safe(String v) {
    return v == null ? "" : v.trim();
  }
}
