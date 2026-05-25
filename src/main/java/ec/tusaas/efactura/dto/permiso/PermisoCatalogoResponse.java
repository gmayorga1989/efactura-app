package ec.tusaas.efactura.dto.permiso;

import java.util.UUID;

public record PermisoCatalogoResponse(UUID id, String codigo, String descripcion, String modulo) {}
