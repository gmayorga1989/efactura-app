package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.cotizacion.CotizacionAdjuntoResponse;
import ec.tusaas.efactura.entity.Cotizacion;
import ec.tusaas.efactura.entity.CotizacionAdjunto;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.CotizacionRepository;
import ec.tusaas.efactura.repository.CotizacionAdjuntoRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.storage.ObjectStorageService;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class CotizacionAdjuntoStorageService {

  private static final long MAX_BYTES = 10 * 1024 * 1024;
  private static final Set<String> TIPOS =
      Set.of(
          "application/pdf",
          "image/png",
          "image/jpeg",
          "image/webp",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          "application/msword",
          "application/vnd.ms-excel");

  private final CotizacionRepository cotizacionRepository;
  private final CotizacionAdjuntoRepository cotizacionAdjuntoRepository;
  private final ObjectStorageService objectStorageService;

  @Transactional
  public CotizacionAdjuntoResponse subirArchivo(
      UUID empresaId, UUID cotizacionId, MultipartFile archivo, UsuarioPrincipal principal) throws Exception {
    validarArchivo(archivo);
    Cotizacion cotizacion =
        cotizacionRepository
            .findByIdAndEmpresa_Id(cotizacionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
    if ("CONVERTIDA".equalsIgnoreCase(cotizacion.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cotización convertida; no se pueden agregar adjuntos");
    }
    Empresa empresa = cotizacion.getEmpresa();
    String extension = extension(archivo.getOriginalFilename(), archivo.getContentType());
    String key =
        "cotizaciones/"
            + empresaId
            + "/"
            + cotizacionId
            + "/"
            + UUID.randomUUID()
            + extension;
    objectStorageService.guardarPublico(key, archivo.getBytes(), archivo.getContentType());
    String url = objectStorageService.publicUrl(key);
    int orden =
        cotizacionAdjuntoRepository.findByCotizacion_IdAndEstadoOrderByOrdenAsc(cotizacionId, "ACTIVO").size();
    CotizacionAdjunto adj = new CotizacionAdjunto();
    adj.setCotizacion(cotizacion);
    adj.setEmpresa(empresa);
    adj.setTipo("ARCHIVO");
    adj.setProveedor("ARCHIVO_LOCAL");
    adj.setTitulo(archivo.getOriginalFilename());
    adj.setUrl(url);
    adj.setStorageKey(key);
    adj.setContentType(archivo.getContentType());
    adj.setTamanoBytes(archivo.getSize());
    adj.setNombreArchivo(archivo.getOriginalFilename());
    adj.setOrden(orden);
    adj = cotizacionAdjuntoRepository.save(adj);
    return toResponse(adj);
  }

  @Transactional
  public void eliminarAdjunto(UUID empresaId, UUID cotizacionId, UUID adjuntoId, UsuarioPrincipal principal)
      throws Exception {
    Cotizacion cotizacion =
        cotizacionRepository
            .findByIdAndEmpresa_Id(cotizacionId, empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cotización no encontrada"));
    if ("CONVERTIDA".equalsIgnoreCase(cotizacion.getEstado())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cotización convertida; no se pueden eliminar adjuntos");
    }
    CotizacionAdjunto adj =
        cotizacionAdjuntoRepository
            .findById(adjuntoId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado"));
    if (adj.getCotizacion() == null
        || !cotizacionId.equals(adj.getCotizacion().getId())
        || adj.getEmpresa() == null
        || !empresaId.equals(adj.getEmpresa().getId())) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Adjunto no encontrado");
    }

    if (adj.getStorageKey() != null && !adj.getStorageKey().isBlank()) {
      objectStorageService.eliminar(adj.getStorageKey());
    }
    cotizacionAdjuntoRepository.delete(adj);
  }

  private static void validarArchivo(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacío");
    }
    if (archivo.getSize() > MAX_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo máximo 10 MB");
    }
    String ct = archivo.getContentType();
    if (ct == null || !TIPOS.contains(ct)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Formato no permitido (PDF, imágenes, Word o Excel)");
    }
  }

  private static String extension(String nombre, String contentType) {
    if (nombre != null) {
      String lower = nombre.toLowerCase();
      if (lower.endsWith(".pdf")) {
        return ".pdf";
      }
      if (lower.endsWith(".png")) {
        return ".png";
      }
      if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
        return ".jpg";
      }
      if (lower.endsWith(".webp")) {
        return ".webp";
      }
      if (lower.endsWith(".docx")) {
        return ".docx";
      }
      if (lower.endsWith(".xlsx")) {
        return ".xlsx";
      }
    }
    return switch (contentType) {
      case "application/pdf" -> ".pdf";
      case "image/png" -> ".png";
      case "image/jpeg" -> ".jpg";
      case "image/webp" -> ".webp";
      default -> ".bin";
    };
  }

  static CotizacionAdjuntoResponse toResponse(CotizacionAdjunto a) {
    return new CotizacionAdjuntoResponse(
        a.getId(),
        a.getTipo(),
        a.getProveedor(),
        a.getTitulo(),
        a.getUrl(),
        a.getNombreArchivo(),
        a.getContentType(),
        a.getTamanoBytes(),
        a.getOrden());
  }
}
