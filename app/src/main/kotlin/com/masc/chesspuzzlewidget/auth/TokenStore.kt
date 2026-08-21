package com.masc.chesspuzzlewidget.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import net.openid.appauth.AuthState

/**
 * Stores the Lichess [AuthState] (access + refresh token, expiry) encrypted at rest.
 * Global (not per-widget) — one Lichess login covers every widget instance.
 */
class TokenStore(context: Context) {

    private val appContext = context.applicationContext

    private val prefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "lichess_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun loadAuthState(): AuthState? {
        val serialized = prefs.getString(KEY_AUTH_STATE, null) ?: return null
        return runCatching { AuthState.jsonDeserialize(serialized) }.getOrNull()
    }

    fun saveAuthState(authState: AuthState) {
        prefs.edit().putString(KEY_AUTH_STATE, authState.jsonSerializeString()).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_AUTH_STATE).apply()
    }

    fun isAuthorized(): Boolean = loadAuthState()?.isAuthorized == true

    companion object {
        private const val KEY_AUTH_STATE = "auth_state"
    }
}
