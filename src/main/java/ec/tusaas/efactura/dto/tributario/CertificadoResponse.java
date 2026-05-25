package ec.tusaas.efactura.dto.tributario;

import java.time.Instant;
import java.util.UUID;

public record CertificadoResponse(
    UUID id,
    String alias,
    String archivoStorageKey,
    String emisor,
    String serial,
    Instant validoDesde,
    Instant validoHasta,
    boolean activoParaFirma,
    String estado) {}
