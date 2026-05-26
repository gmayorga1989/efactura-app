package ec.tusaas.efactura.dto.vendedor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VendedorRequest(
    @Size(max = 30) String codigo,
    @NotBlank @Size(max = 120) String nombres,
    @Size(max = 120) String apellidos,
    @Email @Size(max = 255) String email,
    @Size(max = 30) String telefono,
    @Size(max = 20) String documentoIdentidad,
    @Size(max = 500) String notas,
    @Size(max = 20) String estado) {}
