package ec.tusaas.efactura.security;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class TotpService {

  private static final String BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
  private static final int PERIOD_SECONDS = 30;
  private static final int DIGITS = 6;
  private final SecureRandom secureRandom = new SecureRandom();

  public String generateSecret() {
    byte[] bytes = new byte[20];
    secureRandom.nextBytes(bytes);
    return encodeBase32(bytes);
  }

  public String otpauthUri(String issuer, String email, String secret) {
    String label = encode(issuer + ":" + email);
    return "otpauth://totp/"
        + label
        + "?secret="
        + secret
        + "&issuer="
        + encode(issuer)
        + "&algorithm=SHA1&digits="
        + DIGITS
        + "&period="
        + PERIOD_SECONDS;
  }

  public boolean verify(String secret, String code) {
    if (secret == null || code == null || !code.matches("\\d{6}")) {
      return false;
    }
    long step = Instant.now().getEpochSecond() / PERIOD_SECONDS;
    for (long candidate = step - 1; candidate <= step + 1; candidate++) {
      if (code.equals(generateCode(secret, candidate))) {
        return true;
      }
    }
    return false;
  }

  private String generateCode(String secret, long step) {
    try {
      byte[] key = decodeBase32(secret);
      byte[] msg = ByteBuffer.allocate(8).putLong(step).array();
      Mac mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(key, "HmacSHA1"));
      byte[] hash = mac.doFinal(msg);
      int offset = hash[hash.length - 1] & 0x0f;
      int binary =
          ((hash[offset] & 0x7f) << 24)
              | ((hash[offset + 1] & 0xff) << 16)
              | ((hash[offset + 2] & 0xff) << 8)
              | (hash[offset + 3] & 0xff);
      int otp = binary % 1_000_000;
      return String.format(Locale.ROOT, "%06d", otp);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("No fue posible generar codigo TOTP", e);
    }
  }

  private static String encodeBase32(byte[] data) {
    StringBuilder result = new StringBuilder();
    int buffer = 0;
    int bitsLeft = 0;
    for (byte b : data) {
      buffer = (buffer << 8) | (b & 0xff);
      bitsLeft += 8;
      while (bitsLeft >= 5) {
        result.append(BASE32.charAt((buffer >> (bitsLeft - 5)) & 31));
        bitsLeft -= 5;
      }
    }
    if (bitsLeft > 0) {
      result.append(BASE32.charAt((buffer << (5 - bitsLeft)) & 31));
    }
    return result.toString();
  }

  private static byte[] decodeBase32(String value) {
    String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
    int buffer = 0;
    int bitsLeft = 0;
    byte[] out = new byte[normalized.length() * 5 / 8];
    int index = 0;
    for (char c : normalized.toCharArray()) {
      int val = BASE32.indexOf(c);
      if (val < 0) {
        throw new IllegalArgumentException("Secreto TOTP invalido");
      }
      buffer = (buffer << 5) | val;
      bitsLeft += 5;
      if (bitsLeft >= 8) {
        out[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
        bitsLeft -= 8;
      }
    }
    return out;
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }
}
