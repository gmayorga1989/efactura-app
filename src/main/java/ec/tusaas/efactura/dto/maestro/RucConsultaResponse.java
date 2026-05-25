package ec.tusaas.efactura.dto.maestro;

import java.util.List;
import java.util.Map;

public record RucConsultaResponse(
    String numeroRuc,
    boolean encontrado,
    String razonSocial,
    String nombreComercial,
    String tipoContribuyente,
    String estadoContribuyenteRuc,
    String obligadoLlevarContabilidad,
    String contribuyenteEspecial,
    String regimen,
    String actividadEconomicaPrincipal,
    List<ClienteDireccionResponse> direcciones,
    Map<String, Object> contribuyenteRaw,
    List<Map<String, Object>> establecimientosRaw) {}
