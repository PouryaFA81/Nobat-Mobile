package app.nobat.mobile

import android.app.Application
import app.nobat.mobile.data.AppDatabase
import app.nobat.mobile.locale.AppLocale

class NobatApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }

    override fun onCreate() {
        super.onCreate()
        AppLocale.applyFromPrefs(this)
    }
}
