package ec.tusaas.efactura.util;

import ec.tusaas.efactura.entity.MembresiaEmpresa;
import ec.tusaas.efactura.entity.Permiso;
import ec.tusaas.efactura.entity.Rol;
import java.util.List;

public final class MembresiaPermisos {

  private MembresiaPermisos() {}

  public static List<String> codigos(MembresiaEmpresa membresia) {
    return membresia.getRoles().stream()
        .flatMap(r -> r.getPermisos().stream())
        .map(Permiso::getCodigo)
        .distinct()
        .toList();
  }

  public static List<String> rolesCodigos(MembresiaEmpresa membresia) {
    return membresia.getRoles().stream().map(Rol::getCodigo).distinct().toList();
  }
}
