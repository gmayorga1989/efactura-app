package ec.tusaas.efactura.dto.emision;

import java.util.UUID;

public record PuntoEmisionEmitirOption(
    UUID id, String establecimientoCodigo, String codigo, String nombre) {}
