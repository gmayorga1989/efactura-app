package ec.tusaas.efactura.dto.apikey;

import java.util.List;
import java.util.UUID;

/**
 * {@code plainKey} solo se devuelve en la creación; guárdela de forma segura.
 */
public record ApiKeyCreatedResponse(
    UUID id, String nombre, String prefix, List<String> scopes, String plainKey) {}
