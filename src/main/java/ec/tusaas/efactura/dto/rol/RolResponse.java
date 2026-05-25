package ec.tusaas.efactura.dto.rol;

import java.util.List;
import java.util.UUID;

public record RolResponse(
    UUID id,
    String codigo,
    String nombre,
    boolean sistema,
    String estado,
    long usuariosAsignados,
    List<String> permisosCodigos) {}
