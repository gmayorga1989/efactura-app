package ec.tusaas.efactura.dto.menu;

import java.util.UUID;
import java.util.List;

public record MenuItemResponse(
    UUID id,
    String codigo,
    String padreCodigo,
    int orden,
    String etiqueta,
    String labelKey,
    String fallbackLabel,
    String rutaFront,
    String icono,
    String modulo,
    List<MenuItemResponse> hijos) {}
