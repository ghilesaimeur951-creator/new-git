package com.ghiles.quizubuntu;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores the GitHub access token encrypted with a key that never leaves Android Keystore.
 * No token is embedded in the source code or committed to GitHub.
 */
public final class SecureTokenStore {

    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "ubuntu_git_academy_github_token";
    private static final String PREFS = "secure_github";
    private static final String IV_KEY = "token_iv";
    private static final String DATA_KEY = "token_data";

    private final Context context;

    public SecureTokenStore(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void saveToken(String token) throws Exception {
        if (token == null || token.trim().isEmpty()) {
            clearToken();
            return;
        }

        SecretKey key = getOrCreateKey();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);

        byte[] encrypted = cipher.doFinal(
            token.trim().getBytes(StandardCharsets.UTF_8)
        );

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
            .putString(IV_KEY, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
            .putString(DATA_KEY, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply();
    }

    public synchronized String loadToken() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            String ivText = prefs.getString(IV_KEY, null);
            String dataText = prefs.getString(DATA_KEY, null);

            if (ivText == null || dataText == null) return "";

            byte[] iv = Base64.decode(ivText, Base64.NO_WRAP);
            byte[] encrypted = Base64.decode(dataText, Base64.NO_WRAP);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateKey(),
                new GCMParameterSpec(128, iv)
            );

            byte[] clear = cipher.doFinal(encrypted);
            return new String(clear, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    public synchronized boolean hasToken() {
        return !loadToken().isEmpty();
    }

    public synchronized void clearToken() {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .remove(IV_KEY)
            .remove(DATA_KEY)
            .apply();
    }

    private SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        KeyGenerator generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE
        );

        generator.init(
            new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        );

        return generator.generateKey();
    }
}
