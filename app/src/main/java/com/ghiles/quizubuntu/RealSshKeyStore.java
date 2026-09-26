package com.ghiles.quizubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Real Ed25519 SSH identity used by Ubuntu Lab.
 *
 * The Ed25519 key pair is generated with the bundled Bouncy Castle provider so
 * the result does not silently fall back to RSA on Android devices whose stock
 * provider lacks Ed25519. Only the public key is exposed to the user.
 *
 * The private PKCS#8 bytes are encrypted with AES/GCM. The AES wrapping key is
 * non-exportable and lives in Android Keystore.
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

    private static final Provider ED25519_PROVIDER = new BouncyCastleProvider();

    private final Context context;

    public RealSshKeyStore(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Generates a genuine Ed25519 key pair suitable for GitHub authentication.
     */
    public synchronized KeyInfo generate(String comment) throws Exception {
        String safeComment =
            comment == null || comment.trim().isEmpty()
                ? "ubuntu-git-academy"
                : comment.trim();

        KeyPairGenerator generator = KeyPairGenerator.getInstance(
            "Ed25519",
            ED25519_PROVIDER
        );
        KeyPair pair = generator.generateKeyPair();

        byte[] privateBytes = pair.getPrivate().getEncoded();
        byte[] publicBytes = pair.getPublic().getEncoded();

        if (privateBytes == null || publicBytes == null) {
            throw new IllegalStateException(
                "Le fournisseur cryptographique n'a pas fourni une clé Ed25519 exportable."
            );
        }

        String publicLine = toOpenSshEd25519(pair.getPublic(), safeComment);
        String fingerprint = fingerprint(publicLine);

        SecretKey wrappingKey = getOrCreateWrappingKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey);
        byte[] encrypted = cipher.doFinal(privateBytes);

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(ALGORITHM, "Ed25519")
            .putString(PRIVATE_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(PRIVATE_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
            .putString(PUBLIC_X509, Base64.encodeToString(publicBytes, Base64.NO_WRAP))
            .putString(PUBLIC_LINE, publicLine)
            .putString(FINGERPRINT, fingerprint)
            .apply();

        return new KeyInfo("Ed25519", publicLine, fingerprint);
    }

    public synchronized boolean hasKey() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return "Ed25519".equals(prefs.getString(ALGORITHM, "")) &&
            prefs.contains(PRIVATE_DATA) &&
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

    /**
     * Decrypts the real private key only in memory when JGit needs it.
     */
    public synchronized KeyPair loadKeyPair() throws Exception {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        String algorithm = prefs.getString(ALGORITHM, "");
        String encryptedText = prefs.getString(PRIVATE_DATA, "");
        String ivText = prefs.getString(PRIVATE_IV, "");
        String publicText = prefs.getString(PUBLIC_X509, "");

        if (!"Ed25519".equals(algorithm) ||
            encryptedText.isEmpty() ||
            ivText.isEmpty() ||
            publicText.isEmpty()) {

            throw new IllegalStateException("Aucune clé SSH Ed25519 réelle n'est disponible.");
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

        KeyFactory factory = KeyFactory.getInstance(
            "Ed25519",
            ED25519_PROVIDER
        );

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

    static String toOpenSshEd25519(
        PublicKey publicKey,
        String comment
    ) throws Exception {
        byte[] x509 = publicKey.getEncoded();

        if (x509 == null || x509.length < 32) {
            throw new IllegalStateException("Clé publique Ed25519 invalide.");
        }

        // RFC 8410 SubjectPublicKeyInfo contains the raw 32-byte Ed25519 key
        // at the end. OpenSSH wire format is:
        // string "ssh-ed25519" + string raw-public-key.
        byte[] raw = new byte[32];
        System.arraycopy(x509, x509.length - 32, raw, 0, 32);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        writeSshString(
            out,
            "ssh-ed25519".getBytes(StandardCharsets.US_ASCII)
        );
        writeSshString(out, raw);

        return "ssh-ed25519 " +
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP) +
            " " +
            (comment == null ? "" : comment.trim());
    }

    static String fingerprint(String publicLine) throws Exception {
        String[] parts = publicLine.split("\\s+");

        if (parts.length < 2) {
            throw new IllegalArgumentException("Clé publique OpenSSH invalide.");
        }

        byte[] blob = Base64.decode(parts[1], Base64.NO_WRAP);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(blob);

        return "SHA256:" +
            Base64.encodeToString(
                digest,
                Base64.NO_WRAP | Base64.NO_PADDING
            );
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

    private static void writeSshString(
        ByteArrayOutputStream out,
        byte[] value
    ) {
        byte[] length = ByteBuffer.allocate(4).putInt(value.length).array();
        out.write(length, 0, length.length);
        out.write(value, 0, value.length);
    }
}
