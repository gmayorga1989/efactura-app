package ec.tusaas.efactura.sri.client;

public interface SriRecepcionClient {

  RecepcionResult enviar(String xmlFirmado, short ambienteSri);

  record RecepcionResult(String estado, String mensaje, int httpStatus, String rawResponse) {}
}
