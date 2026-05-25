package ec.tusaas.efactura.sri;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Genera la clave de acceso de 49 dígitos (48 + dígito verificador módulo 11) según ficha técnica SRI Ecuador:
 *
 * <p>fecha(8) + tipoComprobante(2) + ruc(13) + ambiente(1) + establecimiento(3) + puntoEmision(3) +
 * secuencial(9) + codigoNumerico(8) + tipoEmision(1) + dígitoVerificador(1).
 */
public final class ClaveAccesoGenerator {

  private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("ddMMyyyy");

  private ClaveAccesoGenerator() {}

  public static String generar(
      LocalDate fechaEmision,
      String tipoComprobante2,
      String ruc13,
      int ambiente1,
      int tipoEmision1,
      String establecimiento3,
      String puntoEmision3,
      long secuencial,
      String ochoAleatorios) {
    validarLongitud("tipoComprobante", tipoComprobante2, 2);
    validarLongitud("ruc", ruc13, 13);
    validarLongitud("establecimiento", establecimiento3, 3);
    validarLongitud("puntoEmision", puntoEmision3, 3);
    validarLongitud("aleatorio8", ochoAleatorios, 8);
    if (ambiente1 < 1 || ambiente1 > 9) {
      throw new IllegalArgumentException("ambiente debe ser 1 dígito (1=pruebas, 2=producción típico)");
    }
    if (tipoEmision1 < 1 || tipoEmision1 > 9) {
      throw new IllegalArgumentException("tipoEmision debe ser 1 dígito");
    }
    String fecha = fechaEmision.format(FECHA);
    String sec9 = String.format("%09d", Math.floorMod(secuencial, 1_000_000_000L));
    String base48 =
        fecha
            + tipoComprobante2
            + ruc13
            + ambiente1
            + establecimiento3
            + puntoEmision3
            + sec9
            + ochoAleatorios
            + tipoEmision1;
    if (base48.length() != 48) {
      throw new IllegalStateException("clave base debe ser 48 dígitos, obtuvo " + base48.length());
    }
    int dv = digitoVerificadorModulo11(base48);
    return base48 + dv;
  }

  /** Ocho dígitos aleatorios para la clave de acceso. */
  public static String ochoDigitosAleatorios() {
    int n = ThreadLocalRandom.current().nextInt(0, 100_000_000);
    return String.format("%08d", n);
  }

  private static void validarLongitud(String nombre, String v, int len) {
    if (v == null || v.length() != len || !v.chars().allMatch(Character::isDigit)) {
      throw new IllegalArgumentException(nombre + " debe tener " + len + " dígitos numéricos");
    }
  }

  /** Módulo 11 según algoritmo habitual SRI (coeficientes 2,3,4,5,6,7 cíclicos desde la derecha). */
  public static int digitoVerificadorModulo11(String digitos) {
    int[] coef = {2, 3, 4, 5, 6, 7};
    int suma = 0;
    int c = 0;
    for (int i = digitos.length() - 1; i >= 0; i--) {
      int d = digitos.charAt(i) - '0';
      suma += d * coef[c % coef.length];
      c++;
    }
    int mod = suma % 11;
    int dv = 11 - mod;
    if (dv == 11) {
      return 0;
    }
    if (dv == 10) {
      return 1;
    }
    return dv;
  }
}
