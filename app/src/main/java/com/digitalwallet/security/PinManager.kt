package com.digitalwallet.security

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    private val securityManager: SecurityManager,
    private val dataStore: DataStore<Preferences>
) {
    private val APP_PIN_KEY = stringPreferencesKey("app_pin_hash")
    private val BIOMETRIC_ENABLED_KEY = booleanPreferencesKey("biometric_enabled")
    private val PIN_SETUP_KEY = booleanPreferencesKey("pin_setup_done")
    private val USER_NAME_KEY = stringPreferencesKey("user_name")
    private val WALLET_SEEDED_KEY = booleanPreferencesKey("wallets_seeded")

    suspend fun isPinSetup(): Boolean = dataStore.data.map { it[PIN_SETUP_KEY] ?: false }.first()
    suspend fun isWalletSeeded(): Boolean = dataStore.data.map { it[WALLET_SEEDED_KEY] ?: false }.first()
    suspend fun markWalletSeeded() { dataStore.edit { it[WALLET_SEEDED_KEY] = true } }

    suspend fun getUserName(): String = dataStore.data.map { it[USER_NAME_KEY] ?: "Shridhar" }.first()
    suspend fun setUserName(name: String) { dataStore.edit { it[USER_NAME_KEY] = name } }

    suspend fun setAppPin(pin: String, userName: String = "Shridhar") {
        val hash = hashPin(pin)
        val encrypted = securityManager.encrypt(hash)
        dataStore.edit { prefs ->
            prefs[APP_PIN_KEY] = encrypted
            prefs[PIN_SETUP_KEY] = true
            prefs[USER_NAME_KEY] = userName
        }
    }

    suspend fun verifyAppPin(pin: String): Boolean {
        val stored = dataStore.data.map { it[APP_PIN_KEY] }.first() ?: return false
        return try { securityManager.decrypt(stored) == hashPin(pin) } catch (e: Exception) { false }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) { dataStore.edit { it[BIOMETRIC_ENABLED_KEY] = enabled } }
    suspend fun isBiometricEnabled(): Boolean = dataStore.data.map { it[BIOMETRIC_ENABLED_KEY] ?: false }.first()

    private fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest((pin + "dw_pin_salt_v1").toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
