package ec.tusaas.efactura.emision;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256 {

  private Sha256() {}

  public static String hex(String value) {
    return hex(value.getBytes(StandardCharsets.UTF_8));
  }

  public static String hex(byte[] value) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(value));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
