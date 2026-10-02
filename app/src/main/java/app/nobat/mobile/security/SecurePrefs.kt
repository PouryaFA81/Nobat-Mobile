package app.nobat.mobile.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Opens EncryptedSharedPreferences only. Never falls back to ordinary prefs —
 * sensitive credentials must not be stored in plaintext on Keystore failure.
 */
object SecurePrefs {
    fun open(context: Context, fileName: String): SharedPreferences {
        try {
            val masterKey = MasterKey.Builder(context.applicationContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            return EncryptedSharedPreferences.create(
                context.applicationContext,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            throw SecureStorageException(e)
        }
    }
}

/** Thrown when Android Keystore / EncryptedSharedPreferences cannot be opened. */
class SecureStorageException(cause: Throwable? = null) :
    Exception("Secure storage unavailable", cause)
