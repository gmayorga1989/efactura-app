package ec.tusaas.efactura.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuiteTenantBootstrapRequest {

  @NotBlank
  @Size(min = 10, max = 128)
  private String password;

  @NotBlank
  @Size(max = 300)
  private String razonSocial;

  /** RUC ecuatoriano 13 dígitos; si se omite o está vacío se genera uno provisional único (prefijo 999…). */
  @Size(max = 13)
  private String ruc;

  /** Slug URL en eFactura; si se omite se deriva del slug de Identity o de la razón social. */
  @Size(max = 100)
  private String empresaSlug;
}
