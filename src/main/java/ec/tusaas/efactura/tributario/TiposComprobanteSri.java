package ec.tusaas.efactura.tributario;

/** Códigos tipo de comprobante según catálogo SRI (valores fijos de dominio). */
public final class TiposComprobanteSri {

  public static final String FACTURA = "01";
  public static final String LIQUIDACION_COMPRA = "03";
  public static final String NOTA_CREDITO = "04";
  public static final String NOTA_DEBITO = "05";
  public static final String GUIA_REMISION = "06";
  public static final String RETENCION = "07";

  public static final String[] TODOS_ORDENADOS = {
    FACTURA, LIQUIDACION_COMPRA, NOTA_CREDITO, NOTA_DEBITO, GUIA_REMISION, RETENCION
  };

  private TiposComprobanteSri() {}
}
