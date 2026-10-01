package app.nobat.mobile.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory unlocked session + last-selected account id in prefs (not unlocked).
 * Cold start shows the entry list; day-to-day app unlock is PIN/biometric when enabled.
 */
class AccountSession(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _unlockedAccountId = MutableStateFlow<Long?>(null)
    val unlockedAccountId: StateFlow<Long?> = _unlockedAccountId.asStateFlow()

    /** Last profile the user unlocked (for entry highlighting); null if never set. */
    var lastAccountId: Long?
        get() {
            val v = prefs.getLong(KEY_LAST_ACCOUNT, -1L)
            return if (v > 0) v else null
        }
        set(value) {
            prefs.edit().apply {
                if (value != null && value > 0) putLong(KEY_LAST_ACCOUNT, value)
                else remove(KEY_LAST_ACCOUNT)
                apply()
            }
        }

    fun unlock(accountId: Long) {
        _unlockedAccountId.value = accountId
        lastAccountId = accountId
    }

    fun lock() {
        _unlockedAccountId.value = null
    }

    fun isUnlocked(accountId: Long): Boolean =
        _unlockedAccountId.value == accountId

    companion object {
        private const val PREFS = "nobat_prefs"
        private const val KEY_LAST_ACCOUNT = "last_account_id"
    }
}
