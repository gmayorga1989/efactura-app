package ec.tusaas.efactura.config.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "efactura.invitacion")
public class InvitacionProperties {

  /** Días de validez del token de invitación. */
  private int expiracionDias = 7;

  /**
   * Si es true, escribe en el log de servidor el token recién creado (útil hasta tener correo real).
   * Desactivar en producción si la respuesta JSON ya no incluye el token.
   */
  private boolean logTokenServidor = false;
}
