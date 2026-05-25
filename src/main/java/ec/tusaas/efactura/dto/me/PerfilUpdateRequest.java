package ec.tusaas.efactura.dto.me;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record PerfilUpdateRequest(
    @Size(max = 200) String nombre,
    @Size(max = 30) String genero,
    LocalDate fechaNacimiento,
    @Size(min = 2, max = 2) String pais,
    @Size(max = 120) String provincia,
    @Size(max = 120) String canton,
    @Size(max = 120) String ciudad,
    @Size(max = 120) String parroquia,
    @Size(max = 10) String idioma,
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "Moneda debe ser ISO 4217") String moneda,
    @Size(max = 80) String zonaHoraria) {}
