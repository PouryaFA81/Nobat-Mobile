package app.nobat.mobile.locale

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Persists an in-app language override (fa | en | system) and applies it via
 * [AppCompatDelegate.setApplicationLocales] so string resources and layout
 * direction follow the chosen locale — not a hard-coded RTL.
 */
object AppLocale {
    const val FA = "fa"
    const val EN = "en"
    const val SYSTEM = "system"

    private const val PREFS = "nobat_prefs"
    private const val KEY_LANGUAGE = "app_language"

    fun getPreference(context: Context): String =
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, SYSTEM)
            ?: SYSTEM

    /** Effective UI language tag used for strings (`fa` or `en`). */
    fun effectiveLanguage(context: Context): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val tag = if (!appLocales.isEmpty) {
            appLocales[0]?.language
        } else {
            context.resources.configuration.locales[0]?.language
        }
        return if (tag?.startsWith("fa", ignoreCase = true) == true) FA else EN
    }

    fun isPersian(context: Context): Boolean = effectiveLanguage(context) == FA

    fun setPreference(context: Context, tag: String) {
        val normalized = when (tag.lowercase()) {
            FA -> FA
            EN -> EN
            else -> SYSTEM
        }
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, normalized)
            .apply()
        apply(normalized)
    }

    /** Apply stored preference at process start. */
    fun applyFromPrefs(context: Context) {
        apply(getPreference(context))
    }

    private fun apply(tag: String) {
        val locales = when (tag) {
            FA -> LocaleListCompat.forLanguageTags(FA)
            EN -> LocaleListCompat.forLanguageTags(EN)
            else -> LocaleListCompat.getEmptyLocaleList()
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
