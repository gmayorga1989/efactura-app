package ec.tusaas.efactura.crypto;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AesGcmSecretCrypto {

  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_BITS = 128;

  private final SecretKey aesKey;
  private final SecureRandom random = new SecureRandom();

  public AesGcmSecretCrypto(@Value("${efactura.crypto.master-secret:}") String masterSecret) {
    this.aesKey = buildKey(masterSecret);
  }

  private static SecretKey buildKey(String masterSecret) {
    if (masterSecret == null || masterSecret.isBlank()) {
      throw new IllegalStateException("efactura.crypto.master-secret es obligatorio para cifrar certificados");
    }
    try {
      MessageDigest sha = MessageDigest.getInstance("SHA-256");
      byte[] key = sha.digest(masterSecret.getBytes(StandardCharsets.UTF_8));
      return new SecretKeySpec(key, "AES");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String encrypt(String plainText) {
    if (plainText == null) {
      throw new IllegalArgumentException("texto nulo");
    }
    try {
      byte[] iv = new byte[GCM_IV_LENGTH];
      random.nextBytes(iv);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
      ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
      buf.put(iv);
      buf.put(cipherText);
      return Base64.getEncoder().encodeToString(buf.array());
    } catch (Exception e) {
      throw new IllegalStateException("Error cifrando dato", e);
    }
  }

  public String decrypt(String cipherBase64) {
    if (cipherBase64 == null || cipherBase64.isBlank()) {
      throw new IllegalArgumentException("cifrado vacío");
    }
    try {
      byte[] decoded = Base64.getDecoder().decode(cipherBase64);
      ByteBuffer buf = ByteBuffer.wrap(decoded);
      byte[] iv = new byte[GCM_IV_LENGTH];
      buf.get(iv);
      byte[] cipherBytes = new byte[buf.remaining()];
      buf.get(cipherBytes);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
      byte[] plain = cipher.doFinal(cipherBytes);
      return new String(plain, StandardCharsets.UTF_8);
    } catch (Exception e) {
      throw new IllegalStateException("Error descifrando dato", e);
    }
  }
}
