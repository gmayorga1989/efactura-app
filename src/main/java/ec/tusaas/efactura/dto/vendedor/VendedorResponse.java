package ec.tusaas.efactura.dto.vendedor;

import java.util.UUID;

public record VendedorResponse(
    UUID id,
    String codigoInterno,
    String codigo,
    String nombres,
    String apellidos,
    String nombreCompleto,
    String email,
    String telefono,
    String documentoIdentidad,
    String notas,
    String estado) {}
