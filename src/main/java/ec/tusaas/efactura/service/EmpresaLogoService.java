package ec.tusaas.efactura.service;

import ec.tusaas.efactura.dto.empresa.EmpresaLogoResponse;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.storage.ObjectStorageService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmpresaLogoService {

  private static final Set<String> CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");

  private final EmpresaRepository empresaRepository;
  private final EmpresaTributarioService empresaTributarioService;
  private final ObjectStorageService objectStorageService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional
  public EmpresaLogoResponse subir(UUID empresaId, MultipartFile archivo, UsuarioPrincipal principal) throws Exception {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    validar(archivo);
    log.info(
        "Logo empresa: inicio upload empresaId={} archivo={} bytes={} contentType={} usuario={} storage={}",
        empresaId,
        archivo.getOriginalFilename(),
        archivo.getSize(),
        archivo.getContentType(),
        principal.getEmail(),
        objectStorageService.getClass().getSimpleName());
    Empresa empresa =
        empresaRepository
            .findById(empresaId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empresa no encontrada"));
    String extension = extension(archivo.getOriginalFilename(), archivo.getContentType());
    String key = "logos/" + empresaId + "/logo-" + UUID.randomUUID() + extension;
    objectStorageService.guardarPublico(key, archivo.getBytes(), archivo.getContentType());
    empresa.setLogoUrl(objectStorageService.publicUrl(key));
    empresa.setFechaModificacion(Instant.now());
    empresa.setUsuarioModificacion(principal.getEmail());
    empresaRepository.save(empresa);
    dashboardCacheService.evictEmpresa(empresaId);
    log.info("Logo empresa: upload completado empresaId={} key={} logoUrl={}", empresaId, key, empresa.getLogoUrl());
    return new EmpresaLogoResponse(empresa.getId(), empresa.getLogoUrl(), key);
  }

  private static void validar(MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio");
    }
    if (archivo.getSize() > 2 * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Logo maximo 2MB");
    }
    String contentType = archivo.getContentType();
    if (contentType == null || !CONTENT_TYPES.contains(contentType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de logo no permitido");
    }
  }

  private static String extension(String nombre, String contentType) {
    if (nombre != null) {
      String lower = nombre.toLowerCase();
      for (String ext : List.of(".png", ".jpg", ".jpeg", ".webp", ".svg")) {
        if (lower.endsWith(ext)) {
          return ext;
        }
      }
    }
    return switch (contentType) {
      case "image/png" -> ".png";
      case "image/jpeg" -> ".jpg";
      case "image/webp" -> ".webp";
      case "image/svg+xml" -> ".svg";
      default -> ".bin";
    };
  }
}
