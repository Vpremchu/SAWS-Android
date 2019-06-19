/*
    CryptoManager.java - Manages cryptographic tasks
 */

package logic;

import android.util.Base64;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CryptoManager {

    // PUBLIC KEY - PEM FORMAT  --------------------------------------------------------------------
    String publicKeyPem = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAyBzuf2lk86pre7i/ruJx" +
            "LGIuR1Qr3sfNiMAGEzoxg4ZJucJNxJ6aQbKjxTWtI2eIn/2fBVuSnWB0aNDn2/R/" +
            "ZmCTfMT1iHC4bRMQDgdVgpqycu8kPjzXwTUywLLSNB7vmwaGQmM9jdsNo3hWpuDr" +
            "1WGk9LmAcYKTURwSWlPSh2iouCV8A6VqkR5fANAfNXS5vmOTyegA/enF8FwludJx" +
            "OQDfv0AvKNWE2lywFg1jBDrdLnd+HMYeCvVAfH/GDwDCfCIHSbpegxhfWUUdJygO" +
            "lBCIjg14bO20hhFmHctpQvtwebba02KG3rGZ0Fm7aEU1uIpbRelEQMCUVj8a359C" +
            "awIDAQAB" +
            "-----END PUBLIC KEY-----";

    //GET PRIVATE KEY FROM STRING
    public static RSAPrivateKey getPrivateKeyFromString(String key) throws IOException, GeneralSecurityException {
        String privateKeyPEM = key;
        privateKeyPEM = privateKeyPEM.replace("-----BEGIN PRIVATE KEY-----\n", "");
        privateKeyPEM = privateKeyPEM.replace("-----END PRIVATE KEY-----", "");
        byte[] encoded = Base64.decode(privateKeyPEM, Base64.DEFAULT);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        RSAPrivateKey privKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
        return privKey;

    }

    //GET PUBLIC KEY FROM STRING
    public static RSAPublicKey getPublicKeyFromString(String key) throws IOException, GeneralSecurityException {
        String publicKeyPEM = key;
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----\n", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        byte[] encoded = Base64.decode(publicKeyPEM, Base64.DEFAULT);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        RSAPublicKey pubKey = (RSAPublicKey) kf.generatePublic(new X509EncodedKeySpec(encoded));
        return pubKey;
    }

    //SIGN SIGNATURE
    public static String sign(PrivateKey privateKey, String message) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, UnsupportedEncodingException {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initSign(privateKey);
        sign.update(message.getBytes(StandardCharsets.UTF_8));
        return new String(Base64.encode(sign.sign(), Base64.DEFAULT), StandardCharsets.UTF_8);
    }

    //VERIFY SIGNATURE WITH PUBLIC KEY
    public static boolean verify(PublicKey publicKey, String message, String signature) throws SignatureException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException {
        Signature sign = Signature.getInstance("SHA256withRSA");
        sign.initVerify(publicKey);
        sign.update(message.getBytes(StandardCharsets.UTF_8));
        return sign.verify(Base64.decode(signature.getBytes(StandardCharsets.UTF_8), Base64.DEFAULT));
    }

    //ENCRYPT USING PUBLIC KEY
    public static String encrypt(String rawText, PublicKey publicKey) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return Base64.encodeToString(cipher.doFinal(rawText.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
    }

    //DECRYPT USING PRIVATE KEY
    public static String decrypt(String cipherText, PrivateKey privateKey) throws IOException, GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(Base64.decode(cipherText, Base64.DEFAULT)).toString();
    }

    public static byte[] generateAESKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32];
        secureRandom.nextBytes(key);
        return key;
    }

    public static byte[] generateAESIV() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] iv = new byte[16];
        return iv;
    }

    public static String encryptAES(String payload, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] cipherText = cipher.doFinal(payload.getBytes());
        return Base64.encodeToString(cipherText, Base64.DEFAULT);
    }

    public static String decryptAES(String payload, byte[] key, byte[] iv) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        byte[] decodedPayload = Base64.decode(payload, Base64.DEFAULT);
        final Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");

        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec);
        byte[] plainText = cipher.doFinal(decodedPayload);
        return new String(plainText);
    }
}
