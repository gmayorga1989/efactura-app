package ec.tusaas.efactura.dto.emision;

import jakarta.validation.constraints.Size;

public record ReenviarCorreoRequest(@Size(max = 500) String emailReceptor) {}
