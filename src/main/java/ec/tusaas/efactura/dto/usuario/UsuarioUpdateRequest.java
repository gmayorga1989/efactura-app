package ec.tusaas.efactura.dto.usuario;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UsuarioUpdateRequest(
    @Size(max = 200) String nombre,
    @Pattern(regexp = "ACTIVO|INACTIVO|PENDIENTE_CONFIRMACION") String estado,
    List<@Size(max = 50) String> roles) {}
