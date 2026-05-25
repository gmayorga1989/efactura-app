package ec.tusaas.efactura.dto.me;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PerfilResponse(
    UUID identidadId,
    String email,
    String nombre,
    String genero,
    LocalDate fechaNacimiento,
    String pais,
    String provincia,
    String canton,
    String ciudad,
    String parroquia,
    String idioma,
    String moneda,
    String zonaHoraria,
    String avatarUrl,
    Instant ultimoPing,
    boolean enLinea,
    boolean mfaHabilitado) {}
