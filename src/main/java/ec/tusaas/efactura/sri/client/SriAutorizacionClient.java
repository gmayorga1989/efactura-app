package ec.tusaas.efactura.sri.client;

import java.time.Instant;

public interface SriAutorizacionClient {

  AutorizacionResult consultar(String claveAcceso, short ambienteSri);

  record AutorizacionResult(
      String estado, String numeroAutorizacion, Instant fechaAutorizacion, String mensaje, String rawResponse) {}
}
