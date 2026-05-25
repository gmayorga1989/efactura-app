package ec.tusaas.efactura.dto.maestro;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record ProductoCategoriaCreateRequest(
    UUID parentId,
    @NotBlank @Size(max = 50) String codigo,
    @NotBlank @Size(max = 200) String nombre,
    Integer orden) {}
