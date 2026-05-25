package ec.tusaas.efactura.dto.empresa;

import java.util.UUID;

public record EmpresaLogoResponse(UUID empresaId, String logoUrl, String storageKey) {}
