package com.ghiles.quizubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Generates and stores a real SSH key pair for GitHub without ever persisting
 * the private key in plaintext. The private PKCS#8 bytes are encrypted with an
 * AES/GCM key held by Android Keystore.
 *
 * Ed25519 is preferred. If the Android security provider does not expose it on
 * an older device, RSA 3072 is used as a compatibility fallback.
 */
public final class RealSshKeyStore {

    public static final class KeyInfo {
        public final String algorithm;
        public final String publicKey;
        public final String fingerprint;

        KeyInfo(String algorithm, String publicKey, String fingerprint) {
            this.algorithm = algorithm;
            this.publicKey = publicKey;
            this.fingerprint = fingerprint;
        }
    }

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String AES_ALIAS = "ubuntu_git_academy_ssh_private_wrapper";
    private static final String PREFS = "real_ssh";
    private static final String ALGORITHM = "algorithm";
    private static final String PRIVATE_DATA = "private_data";
    private static final String PRIVATE_IV = "private_iv";
    private static final String PUBLIC_X509 = "public_x509";
    private static final String PUBLIC_LINE = "public_line";
    private static final String FINGERPRINT = "fingerprint";

    private final Context context;

    public RealSshKeyStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized KeyInfo generate(String comment) throws Exception {
        String safeComment =
            comment == null || comment.trim().isEmpty()
                ? "ubuntu-git-academy"
                : comment.trim();

        KeyPair pair;
        String algorithm;

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("Ed25519");
            pair = generator.generateKeyPair();
            algorithm = "Ed25519";
        } catch (Exception unsupported) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(3072);
            pair = generator.generateKeyPair();
            algorithm = "RSA";
        }

        byte[] privateBytes = pair.getPrivate().getEncoded();
        byte[] publicBytes = pair.getPublic().getEncoded();

        if (privateBytes == null || publicBytes == null) {
            throw new IllegalStateException(
                "Le fournisseur cryptographique Android ne permet pas d'exporter la clé."
            );
        }

        String publicLine = toOpenSshPublicKey(pair.getPublic(), algorithm, safeComment);
        String fingerprint = fingerprint(publicLine);

        SecretKey wrappingKey = getOrCreateWrappingKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey);
        byte[] encrypted = cipher.doFinal(privateBytes);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ALGORITHM, algorithm)
            .putString(PRIVATE_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PRIVATE_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
            .putString(PUBLIC_X509, Base64.encodeToString(publicBytes, Base64.NO_WRAP))
            .putString(PUBLIC_LINE, publicLine)
            .putString(FINGERPRINT, fingerprint)
            .apply();

        return new KeyInfo(algorithm, publicLine, fingerprint);
    }

    public synchronized boolean hasKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.contains(PRIVATE_DATA) &&
            prefs.contains(PUBLIC_X509) &&
            prefs.contains(PUBLIC_LINE);
    }

    public synchronized KeyInfo info() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        return new KeyInfo(
            prefs.getString(ALGORITHM, ""),
            prefs.getString(PUBLIC_LINE, ""),
            prefs.getString(FINGERPRINT, "")
        );
    }

    public synchronized KeyPair loadKeyPair() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String algorithm = prefs.getString(ALGORITHM, "");
        String encryptedText = prefs.getString(PRIVATE_DATA, "");
        String ivText = prefs.getString(PRIVATE_IV, "");
        String publicText = prefs.getString(PUBLIC_X509, "");

        if (algorithm.isEmpty() ||
            encryptedText.isEmpty() ||
            ivText.isEmpty() ||
            publicText.isEmpty()) {

            throw new IllegalStateException("Aucune clé SSH réelle n'est disponible.");
        }

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateWrappingKey(),
            new GCMParameterSpec(
                128,
                Base64.decode(ivText, Base64.NO_WRAP)
            )
        );

        byte[] privateBytes = cipher.doFinal(
            Base64.decode(encryptedText, Base64.NO_WRAP)
        );
        byte[] publicBytes = Base64.decode(publicText, Base64.NO_WRAP);

        KeyFactory factory = KeyFactory.getInstance(algorithm);

        PrivateKey privateKey = factory.generatePrivate(
            new PKCS8EncodedKeySpec(privateBytes)
        );
        PublicKey publicKey = factory.generatePublic(
            new X509EncodedKeySpec(publicBytes)
        );

        return new KeyPair(publicKey, privateKey);
    }

    public synchronized void clear() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }

    private SecretKey getOrCreateWrappingKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(AES_ALIAS)) {
            return (SecretKey) keyStore.getKey(AES_ALIAS, null);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        );

        generator.init(
            new KeyGenParameterSpec.Builder(
                AES_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        );

        return generator.generateKey();
    }

    private String toOpenSshPublicKey(
        PublicKey publicKey,
        String algorithm,
        String comment
    ) throws Exception {

        byte[] blob;
        String prefix;

        if ("RSA".equals(algorithm)) {
            RSAPublicKey rsa = (RSAPublicKey) publicKey;

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeSshString(out, "ssh-rsa".getBytes(StandardCharsets.US_ASCII));
            writeMpInt(out, rsa.getPublicExponent());
            writeMpInt(out, rsa.getModulus());

            blob = out.toByteArray();
            prefix = "ssh-rsa";
        } else {
            // RFC 8410 SubjectPublicKeyInfo for Ed25519 ends with the raw
            // 32-byte public key. This lets us produce the OpenSSH wire format
            // without writing the private key to disk.
            byte[] x509 = publicKey.getEncoded();

            if (x509 == null || x509.length < 32) {
                throw new IllegalStateException("Clé publique Ed25519 invalide.");
            }

            byte[] raw = new byte[32];
            System.arraycopy(x509, x509.length - 32, raw, 0, 32);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeSshString(
                out,
                "ssh-ed25519".getBytes(StandardCharsets.US_ASCII)
            );
            writeSshString(out, raw);

            blob = out.toByteArray();
            prefix = "ssh-ed25519";
        }

        return prefix + " " +
            Base64.encodeToString(blob, Base64.NO_WRAP) +
            " " +
            comment;
    }

    private String fingerprint(String publicLine) throws Exception {
        String[] parts = publicLine.split("\\s+");

        if (parts.length < 2) return "";

        byte[] blob = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(blob);

        return "SHA256:" +
            Base64.encodeToString(
                digest,
                Base64.NO_WRAP | Base64.NO_PADDING
            );
    }

    private void writeSshString(ByteArrayOutputStream out, byte[] value) {
        byte[] length = ByteBuffer.allocate(4).putInt(value.length).array();
        out.write(length, 0, length.length);
        out.write(value, 0, value.length);
    }

    private void writeMpInt(ByteArrayOutputStream out, BigInteger value) {
        byte[] data = value.toByteArray();
        writeSshString(out, data);
    }
}
