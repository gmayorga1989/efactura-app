package ec.tusaas.efactura.dto.cotizacion;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CotizacionEnviarCorreoRequest(
    @NotEmpty List<@Email String> destinatarios, String asunto, String mensajeAdicional) {}
