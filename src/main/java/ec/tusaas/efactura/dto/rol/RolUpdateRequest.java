package ec.tusaas.efactura.dto.rol;

import jakarta.validation.constraints.Size;
import java.util.List;

public record RolUpdateRequest(@Size(max = 150) String nombre, List<String> permisosCodigos) {}
