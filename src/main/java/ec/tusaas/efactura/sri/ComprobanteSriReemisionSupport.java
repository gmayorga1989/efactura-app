package ec.tusaas.efactura.sri;

import ec.tusaas.efactura.emision.EstadoSri;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Reglas para reenviar un comprobante ya emitido al SRI sin cambiar clave ni numeración. */
public final class ComprobanteSriReemisionSupport {

  private ComprobanteSriReemisionSupport() {}

  public static boolean puedeReemitir(String estadoSri) {
    String s = normalizar(estadoSri);
    return EstadoSri.ERROR.equals(s)
        || EstadoSri.DEVUELTO.equals(s)
        || "DEVUELTA".equals(s)
        || EstadoSri.NO_AUTORIZADO.equals(s)
        || "NO AUTORIZADO".equalsIgnoreCase(estadoSri == null ? "" : estadoSri.trim())
        || "RECHAZADA".equals(s)
        || "NO_AUTORIZADO".equals(s);
  }

  public static boolean puedeReconsultarAutorizacion(String estadoSri) {
    String s = normalizar(estadoSri);
    return EstadoSri.PENDIENTE_AUTORIZACION.equals(s) || "RECIBIDA".equals(s);
  }

  public static void validarPuedeReemitir(String estadoSri) {
    if (!puedeReemitir(estadoSri)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "El comprobante no puede reenviarse al SRI en estado " + estadoSri
              + ". Solo aplica en devuelto, error o no autorizado.");
    }
  }

  private static String normalizar(String estadoSri) {
    if (estadoSri == null || estadoSri.isBlank()) {
      return "";
    }
    return estadoSri.trim().toUpperCase().replace(' ', '_');
  }
}
