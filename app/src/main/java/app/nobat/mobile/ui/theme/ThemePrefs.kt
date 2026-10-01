package app.nobat.mobile.ui.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide appearance: Dark (default) or Light.
 * Persisted in SharedPreferences; [darkTheme] drives [NobatTheme].
 */
object ThemePrefs {
    private const val PREFS = "nobat_prefs"
    private const val KEY_DARK = "theme_dark"

    private val _darkTheme = MutableStateFlow(true)
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    fun init(context: Context) {
        _darkTheme.value = prefs(context).getBoolean(KEY_DARK, true)
    }

    fun isDark(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DARK, true)

    fun setDark(context: Context, dark: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, dark).apply()
        _darkTheme.value = dark
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
