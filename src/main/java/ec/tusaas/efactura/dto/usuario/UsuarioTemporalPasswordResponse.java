package ec.tusaas.efactura.dto.usuario;

import java.util.UUID;

public record UsuarioTemporalPasswordResponse(
    UUID membresiaId, String estado, boolean emailEnviado) {}
