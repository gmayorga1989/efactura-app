package ec.tusaas.efactura.util;

import ec.tusaas.efactura.entity.Comprobante;
import ec.tusaas.efactura.entity.Vendedor;
import java.util.UUID;

public final class ComprobanteVendedorMapper {

  private ComprobanteVendedorMapper() {}

  public static UUID vendedorId(Comprobante c) {
    return c.getVendedor() != null ? c.getVendedor().getId() : null;
  }

  public static String vendedorNombre(Comprobante c) {
    return nombreCompleto(c.getVendedor());
  }

  public static String nombreCompleto(Vendedor v) {
    if (v == null) {
      return null;
    }
    String n = v.getNombres() != null ? v.getNombres().trim() : "";
    String a = v.getApellidos() != null ? v.getApellidos().trim() : "";
    String full = (n + " " + a).trim();
    return full.isEmpty() ? null : full;
  }
}
