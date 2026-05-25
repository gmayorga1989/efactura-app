package ec.tusaas.efactura.dto.me;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;

@Schema(description = "Banderas derivadas de permisos para el front (evitar hardcode extensivo)")
public record MeFeatures(
    @Schema(description = "EMPRESA_ADMIN u otros meta") boolean puedeAdministrarEmpresa,
    boolean puedeEmitir,
    boolean puedeGestionarVentas,
    boolean puedeGestionarProveedores,
    boolean puedeMonitorComprobantes,
    boolean puedeVerReportes,
    boolean puedeGestionarUsuarios,
    boolean puedeApiKeys,
    boolean esPlataforma,
    @Schema(description = "Permiso SUITE_APP_CARTERA o administrador de plataforma")
        boolean puedeAbrirCarteraSuite,
    @Schema(description = "Permiso SUITE_APP_POS o administrador de plataforma")
        boolean puedeAbrirPosSuite) {

  public static MeFeatures fromPermisos(Set<String> permisos, boolean esPlataforma) {
    boolean adminEmpresa = permisos.contains("EMPRESA_ADMIN");
    boolean platform = permisos.contains("PLATFORM_ADMIN");
    return new MeFeatures(
        adminEmpresa || platform,
        permisos.contains("FACTURA_EMITIR") || adminEmpresa || platform,
        permisos.contains("VENTAS_GESTIONAR") || adminEmpresa || platform,
        permisos.contains("PROVEEDOR_GESTIONAR") || adminEmpresa || platform,
        permisos.contains("COMPROBANTE_MONITOR") || adminEmpresa || platform,
        permisos.contains("REPORTE_VER") || adminEmpresa || platform,
        adminEmpresa || platform,
        adminEmpresa || platform,
        esPlataforma,
        permisos.contains("SUITE_APP_CARTERA") || platform,
        permisos.contains("SUITE_APP_POS") || platform);
  }
}
