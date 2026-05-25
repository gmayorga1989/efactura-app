package ec.tusaas.efactura.service;

import ec.tusaas.efactura.crypto.AesGcmSecretCrypto;
import ec.tusaas.efactura.dto.tributario.CertificadoResponse;
import ec.tusaas.efactura.entity.Certificado;
import ec.tusaas.efactura.entity.Empresa;
import ec.tusaas.efactura.repository.CertificadoRepository;
import ec.tusaas.efactura.repository.EmpresaRepository;
import ec.tusaas.efactura.security.UsuarioPrincipal;
import ec.tusaas.efactura.storage.LocalCertificadoStorage;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
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
public class CertificadoService {

  private final CertificadoRepository certificadoRepository;
  private final EmpresaRepository empresaRepository;
  private final LocalCertificadoStorage localCertificadoStorage;
  private final AesGcmSecretCrypto aesGcmSecretCrypto;
  private final EmpresaTributarioService empresaTributarioService;
  private final DashboardCacheService dashboardCacheService;

  @Transactional(readOnly = true)
  public List<CertificadoResponse> listar(UUID empresaId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    return certificadoRepository.findByEmpresa_IdOrderByFechaCreacionDesc(empresaId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public CertificadoResponse subir(
      UUID empresaId, MultipartFile archivo, String passwordPlano, String alias, UsuarioPrincipal principal)
      throws Exception {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    validarExtension(archivo.getOriginalFilename());
    log.info(
        "Certificado: inicio upload empresaId={} archivo={} bytes={} usuario={}",
        empresaId,
        archivo.getOriginalFilename(),
        archivo.getSize(),
        principal.getEmail());
    Empresa empresa =
        empresaRepository.findById(empresaId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    byte[] bytes = archivo.getBytes();
    if (bytes.length == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Archivo vacio");
    }

    CertificadoValidado certificadoValidado = validarPkcs12(bytes, passwordPlano);
    String aliasCert = certificadoValidado.alias();
    X509Certificate x509 = certificadoValidado.certificado();

    String storageKey = localCertificadoStorage.guardar(empresaId, bytes, archivo.getOriginalFilename());
    log.info(
        "Certificado: archivo almacenado empresaId={} storageKey={} aliasCert={} serial={}",
        empresaId,
        storageKey,
        aliasCert,
        x509.getSerialNumber().toString(16));
    Certificado c = new Certificado();
    c.setEmpresa(empresa);
    c.setAlias(alias != null && !alias.isBlank() ? alias : aliasCert);
    c.setArchivoStorageKey(storageKey);
    c.setPasswordCifrado(aesGcmSecretCrypto.encrypt(passwordPlano));
    c.setEmisor(x509.getIssuerX500Principal().getName());
    c.setSerial(x509.getSerialNumber().toString(16));
    c.setValidoDesde(x509.getNotBefore().toInstant());
    c.setValidoHasta(x509.getNotAfter().toInstant());
    c.setActivoParaFirma(false);
    Certificado saved = certificadoRepository.save(c);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  @Transactional
  public CertificadoResponse activar(UUID empresaId, UUID certificadoId, UsuarioPrincipal principal) {
    empresaTributarioService.validarGestionEmpresa(empresaId, principal);
    Certificado target =
        certificadoRepository
            .findById(certificadoId)
            .filter(c -> c.getEmpresa().getId().equals(empresaId))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    certificadoRepository
        .findFirstByEmpresa_IdAndActivoParaFirmaIsTrue(empresaId)
        .ifPresent(
            c -> {
              c.setActivoParaFirma(false);
              certificadoRepository.save(c);
            });
    target.setActivoParaFirma(true);
    log.info("Certificado: activado empresaId={} certificadoId={}", empresaId, certificadoId);
    Certificado saved = certificadoRepository.save(target);
    dashboardCacheService.evictEmpresa(empresaId);
    return toResponse(saved);
  }

  private void validarExtension(String nombre) {
    if (nombre == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de archivo requerido");
    }
    String n = nombre.toLowerCase();
    if (!n.endsWith(".p12") && !n.endsWith(".pfx")) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se permiten archivos .p12 o .pfx");
    }
  }

  private CertificadoValidado validarPkcs12(byte[] bytes, String passwordPlano) throws Exception {
    if (passwordPlano == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contrasena del certificado requerida");
    }

    char[] password = passwordPlano.toCharArray();
    KeyStore ks = KeyStore.getInstance("PKCS12");
    try {
      ks.load(new ByteArrayInputStream(bytes), password);
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo abrir el PKCS12 (contrasena o formato)");
    }

    Enumeration<String> aliases = ks.aliases();
    while (aliases.hasMoreElements()) {
      String alias = aliases.nextElement();
      if (!ks.isKeyEntry(alias)) {
        continue;
      }
      if (!(ks.getKey(alias, password) instanceof PrivateKey)) {
        continue;
      }
      if (ks.getCertificate(alias) instanceof X509Certificate x509) {
        return new CertificadoValidado(alias, x509);
      }
    }

    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "El PKCS12 no contiene una llave privada y certificado X509 utilizables");
  }

  private record CertificadoValidado(String alias, X509Certificate certificado) {}

  private CertificadoResponse toResponse(Certificado c) {
    return new CertificadoResponse(
        c.getId(),
        c.getAlias(),
        c.getArchivoStorageKey(),
        c.getEmisor(),
        c.getSerial(),
        c.getValidoDesde(),
        c.getValidoHasta(),
        c.isActivoParaFirma(),
        c.getEstado());
  }
}
