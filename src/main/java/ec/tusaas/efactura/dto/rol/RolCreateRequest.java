package ec.tusaas.efactura.dto.rol;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RolCreateRequest(
    @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "Solo mayúsculas, números y guión bajo")
        String codigo,
    @NotBlank @Size(max = 150) String nombre,
    List<String> permisosCodigos) {}
