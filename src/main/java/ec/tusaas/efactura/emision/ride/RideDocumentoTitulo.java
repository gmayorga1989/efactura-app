package ec.tusaas.efactura.emision.ride;

/** Títulos del recuadro principal del RIDE según tipo de comprobante. */
public record RideDocumentoTitulo(String linea1, String linea2) {

  public static RideDocumentoTitulo fromTipo(String tipo) {
    String t = tipo == null ? "" : tipo.trim().toUpperCase();
    return switch (t) {
      case "NOTA_CREDITO" -> new RideDocumentoTitulo("NOTA DE", "CRÉDITO");
      case "NOTA_DEBITO" -> new RideDocumentoTitulo("NOTA DE", "DÉBITO");
      case "GUIA_REMISION" -> new RideDocumentoTitulo("GUÍA DE", "REMISIÓN");
      case "RETENCION" -> new RideDocumentoTitulo("COMPROBANTE", "RETENCIÓN");
      case "LIQUIDACION_COMPRA" -> new RideDocumentoTitulo("LIQUIDACIÓN", "DE COMPRA");
      default -> new RideDocumentoTitulo("FACTURA", "ELECTRÓNICA");
    };
  }

  public static String normalizarTipo(String tipo) {
    String t = tipo == null ? "" : tipo.trim().toUpperCase();
    return switch (t) {
      case "NOTA_CREDITO", "NOTA_DEBITO", "GUIA_REMISION", "RETENCION", "LIQUIDACION_COMPRA" -> t;
      default -> "FACTURA";
    };
  }
}
