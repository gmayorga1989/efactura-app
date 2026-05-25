package ec.tusaas.efactura.dto.maestro;

import java.util.UUID;

public record ProductoCategoriaResponse(
    UUID id,
    UUID parentId,
    String codigo,
    String nombre,
    int nivel,
    String ruta,
    int orden) {}
