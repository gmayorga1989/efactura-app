package ec.tusaas.efactura.dto.me;

import ec.tusaas.efactura.dto.empresa.EmpresaResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

@Schema(description = "Contexto de sesión: identidad + empresa actual + permisos efectivos del JWT")
public record MeResponse(
    @Schema(description = "Identidad de login (subject del JWT)") UUID identidadId,
    @Schema(description = "Membresía usada para permisos actuales", nullable = true) UUID membresiaId,
    String email,
    String nombre,
    @Schema(nullable = true, description = "Empresa del JWT; null en contexto plataforma") UUID empresaId,
    EmpresaResponse empresa,
    List<String> permisos,
    List<String> roles,
    MeFeatures features,
    PerfilResponse perfil,
    @Schema(
            description =
                "Fuente de verdad de navegación: permisos/roles + convención en frontend. Lista vacía = usar solo features/permisos.")
    List<String> menuHints) {}
