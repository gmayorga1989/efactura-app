package ec.tusaas.efactura.dto.storage;

public record StorageStatusResponse(
    String provider,
    String activeImplementation,
    String objectRoot,
    String bucket,
    String region,
    String endpoint,
    String publicBaseUrl,
    boolean accessKeyConfigured,
    boolean secretKeyConfigured) {}
