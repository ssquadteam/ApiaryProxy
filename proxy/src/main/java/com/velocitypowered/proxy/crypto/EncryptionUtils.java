/*
 * Copyright (C) 2022-2023 Velocity Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.velocitypowered.proxy.crypto;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteStreams;
import com.velocitypowered.proxy.util.except.QuietDecoderException;
import it.unimi.dsi.fastutil.Pair;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Cipher;

/**
 * Generic utilities for dealing with encryption operations in Minecraft.
 */
public enum EncryptionUtils {
  ;

  public static final Pair<String, String> PEM_RSA_PUBLIC_KEY_DESCRIPTOR =
      Pair.of("-----BEGIN RSA PUBLIC KEY-----", "-----END RSA PUBLIC KEY-----");
  public static final Pair<String, String> PEM_RSA_PRIVATE_KEY_DESCRIPTOR =
      Pair.of("-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");

  public static final String SHA1_WITH_RSA = "SHA1withRSA";
  public static final String SHA256_WITH_RSA = "SHA256withRSA";

  public static final QuietDecoderException INVALID_SIGNATURE
      = new QuietDecoderException("Incorrectly signed chat message");
  public static final QuietDecoderException PREVIEW_SIGNATURE_MISSING
      = new QuietDecoderException("Unsigned chat message requested signed preview");
  public static final byte[] EMPTY = new byte[0];
  private static final PublicKey YGGDRASIL_SESSION_KEY;
  private static final KeyFactory RSA_KEY_FACTORY;

  private static final Base64.Encoder MIME_SPECIAL_ENCODER
      = Base64.getMimeEncoder(76, "\n".getBytes(StandardCharsets.UTF_8));

  static {
    try {
      RSA_KEY_FACTORY = KeyFactory.getInstance("RSA");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }

    try {
      byte[] bytes = ByteStreams.toByteArray(
          Objects.requireNonNull(EncryptionUtils.class.getClassLoader()
              .getResourceAsStream("yggdrasil_session_pubkey.der")));
      YGGDRASIL_SESSION_KEY = parseRsaPublicKey(bytes);
    } catch (IOException | NullPointerException err) {
      throw new RuntimeException(err);
    }
  }

  public static PublicKey getYggdrasilSessionKey() {
    return YGGDRASIL_SESSION_KEY;
  }

  /**
   * Verifies a key signature.
   *
   * @param algorithm the signature algorithm
   * @param base      the public key to verify with
   * @param signature the signature to verify against
   * @param toVerify  the byte array(s) of data to verify
   * @return validity of the signature
   */
  public static boolean verifySignature(final String algorithm, final PublicKey base, final byte[] signature,
      final byte[]... toVerify) {
    Preconditions.checkArgument(toVerify.length > 0);
    try {
      Signature construct = Signature.getInstance(algorithm);
      construct.initVerify(base);
      for (byte[] bytes : toVerify) {
        construct.update(bytes);
      }
      return construct.verify(signature);
    } catch (GeneralSecurityException e) {
      throw new IllegalArgumentException("Invalid signature parameters");
    }
  }

  public static String encodeUrlEncoded(final byte[] data) {
    return MIME_SPECIAL_ENCODER.encodeToString(data);
  }

  public static byte[] decodeUrlEncoded(final String toParse) {
    return Base64.getMimeDecoder().decode(toParse);
  }

  /**
   * Encodes an RSA key as String cer format.
   *
   * @param toEncode the private or public RSA key
   * @return the encoded key cer
   */
  public static String pemEncodeRsaKey(final Key toEncode) {
    Preconditions.checkNotNull(toEncode);
    Pair<String, String> encoder;
    if (toEncode instanceof PublicKey) {
      encoder = PEM_RSA_PUBLIC_KEY_DESCRIPTOR;
    } else if (toEncode instanceof PrivateKey) {
      encoder = PEM_RSA_PRIVATE_KEY_DESCRIPTOR;
    } else {
      throw new IllegalArgumentException("Invalid key type");
    }

    return encoder.first() + "\n"
        + encodeUrlEncoded(toEncode.getEncoded()) + "\n"
        + encoder.second() + "\n";
  }

  /**
   * Parse an RSA public key from key bytes.
   *
   * @param keyValue the key bytes
   * @return the generated key
   */
  public static PublicKey parseRsaPublicKey(final byte[] keyValue) {
    try {
      return RSA_KEY_FACTORY.generatePublic(new X509EncodedKeySpec(keyValue));
    } catch (InvalidKeySpecException e) {
      throw new IllegalArgumentException("Invalid key bytes");
    }
  }

  /**
   * Generates an RSA key pair.
   *
   * @param keysize the key size (in bits) for the RSA key pair
   * @return the generated key pair
   */
  public static KeyPair createRsaKeyPair(final int keysize) {
    try {
      final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(keysize);
      return generator.generateKeyPair();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("Unable to generate RSA keypair", e);
    }
  }

  /**
   * Generates a hex digest in two's complement form for use with the Mojang joinedServer endpoint.
   *
   * @param digest the bytes to digest
   * @return the hex digest
   */
  public static String twosComplementHexdigest(final byte[] digest) {
    return new BigInteger(digest).toString(16);
  }

  /**
   * Decrypts an RSA message.
   *
   * @param keyPair the key pair to use
   * @param bytes   the bytes of the encrypted message
   * @return the decrypted message
   * @throws GeneralSecurityException if the message couldn't be decoded
   */
  public static byte[] decryptRsa(final KeyPair keyPair, final byte[] bytes) throws GeneralSecurityException {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
    return cipher.doFinal(bytes);
  }

  /**
   * Generates the server ID for the hasJoined endpoint.
   *
   * @param sharedSecret the shared secret between the client and the proxy
   * @param key          the RSA public key
   * @return the server ID
   */
  public static String generateServerId(final byte[] sharedSecret, final PublicKey key) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      digest.update(sharedSecret);
      digest.update(key.getEncoded());
      return twosComplementHexdigest(digest.digest());
    } catch (NoSuchAlgorithmException e) {
      throw new AssertionError(e);
    }
  }
}
