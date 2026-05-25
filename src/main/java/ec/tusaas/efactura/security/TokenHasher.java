package ec.tusaas.efactura.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class TokenHasher {

  private static final SecureRandom RANDOM = new SecureRandom();

  private TokenHasher() {}

  public static String randomRefreshTokenPlain() {
    byte[] buf = new byte[32];
    RANDOM.nextBytes(buf);
    return HexFormat.of().formatHex(buf);
  }

  public static String sha256Hex(String plain) {
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(md.digest(plain.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
