package ec.tusaas.efactura.dto.maestro;

import java.util.UUID;

public record ListaPrecioResponse(UUID id, String codigo, String nombre, boolean esDefault) {}
