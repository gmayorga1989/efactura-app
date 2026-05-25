package ec.tusaas.efactura.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantDatasourceConfigRequest(
    @NotBlank @Pattern(regexp = "SHARED|DEDICATED") String modoTenant,
    @NotBlank @Size(max = 120) String datasourceKey,
    @Size(max = 20) String estado) {}
