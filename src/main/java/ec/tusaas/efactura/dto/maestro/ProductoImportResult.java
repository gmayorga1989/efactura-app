package ec.tusaas.efactura.dto.maestro;

import java.util.List;

public record ProductoImportResult(
    int totalFilas, int creados, int actualizados, int errores, List<ProductoImportLineResult> detalles) {}
