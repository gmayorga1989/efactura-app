package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProductoCategoriaUpdateRequest(
    UUID parentId, @Size(max = 200) String nombre, Integer orden) {}
