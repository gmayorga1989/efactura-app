package ec.tusaas.efactura.dto.factura;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FacturaCampoExtraItem(
    @NotBlank @Size(max = 64) @Pattern(regexp = "[a-zA-Z0-9_\\-]+") String codigo,
    @NotBlank @Size(max = 200) String etiqueta,
    @NotBlank @Pattern(regexp = "text|number|select") String tipo,
    boolean requerido,
    List<String> opciones) {}
