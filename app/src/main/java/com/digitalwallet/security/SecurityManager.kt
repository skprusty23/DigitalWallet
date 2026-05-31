package com.digitalwallet.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecurityManager @Inject constructor(@ApplicationContext private val context: Context) {

    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val securePrefs: SharedPreferences
        get() = context.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    fun getOrCreateDatabaseKey(): ByteArray {
        val prefs = securePrefs
        val storedBlob = prefs.getString(DB_KEY_BLOB_PREF, null)
        val androidKey = getOrCreateSecretKey(DB_KEY_ALIAS)
        if (storedBlob != null) {
            return try {
                decryptKeyBlob(storedBlob, androidKey)
            } catch (e: Exception) {
                deleteExistingDatabase()
                prefs.edit().remove(DB_KEY_BLOB_PREF).apply()
                generateAndPersistDatabaseKey(prefs, getOrCreateSecretKey(DB_KEY_ALIAS))
            }
        }
        deleteExistingDatabase()
        return generateAndPersistDatabaseKey(prefs, androidKey)
    }

    fun encrypt(plainText: String): String {
        val key = getOrCreateSecretKey(DEFAULT_KEY_ALIAS)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(GCM_IV_LENGTH + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
        System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedText: String): String {
        val key = getOrCreateSecretKey(DEFAULT_KEY_ALIAS)
        val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return String(cipher.doFinal(encrypted), Charsets.UTF_8)
    }

    private fun generateAndPersistDatabaseKey(prefs: SharedPreferences, androidKey: SecretKey): ByteArray {
        val rawKey = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val blob = encryptKeyBlob(rawKey, androidKey)
        prefs.edit().putString(DB_KEY_BLOB_PREF, blob).apply()
        return rawKey
    }

    private fun encryptKeyBlob(rawKey: ByteArray, secretKey: SecretKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(rawKey)
        val combined = ByteArray(GCM_IV_LENGTH + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH)
        System.arraycopy(encrypted, 0, combined, GCM_IV_LENGTH, encrypted.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptKeyBlob(blob: String, secretKey: SecretKey): ByteArray {
        val combined = Base64.decode(blob, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, GCM_IV_LENGTH)
        val encrypted = combined.copyOfRange(GCM_IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH * 8, iv))
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateSecretKey(alias: String): SecretKey {
        keyStore.getKey(alias, null)?.let { return it as SecretKey }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        keyGenerator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return keyGenerator.generateKey()
    }

    private fun deleteExistingDatabase() {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (dbFile.exists()) {
            dbFile.delete()
            File("${dbFile.path}-wal").delete()
            File("${dbFile.path}-shm").delete()
        }
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 16
        private const val DB_KEY_ALIAS = "dw_db_key"
        private const val DEFAULT_KEY_ALIAS = "dw_default_key"
        private const val SECURE_PREFS_NAME = "dw_secure_keystore_prefs"
        private const val DB_KEY_BLOB_PREF = "db_key_blob"
        const val DB_NAME = "digital_wallet.db"
    }
}
