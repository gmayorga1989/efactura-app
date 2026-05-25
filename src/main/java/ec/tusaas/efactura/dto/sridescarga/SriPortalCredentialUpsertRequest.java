package ec.tusaas.efactura.dto.sridescarga;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SriPortalCredentialUpsertRequest(
    @NotBlank @Size(max = 100) String portalUsuario, @NotBlank @Size(min = 4, max = 200) String portalClave) {}
