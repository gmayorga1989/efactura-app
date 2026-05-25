package ec.tusaas.efactura.dto.apikey;

import jakarta.validation.constraints.Size;
import java.util.List;

public record ApiKeyCreateRequest(
    @Size(max = 150) String nombre, List<@Size(max = 64) String> scopes) {}
