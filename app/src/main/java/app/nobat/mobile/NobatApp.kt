package app.nobat.mobile

import android.app.Application
import app.nobat.mobile.data.AccountRepository
import app.nobat.mobile.data.AppDatabase
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.security.AppLockStore
import app.nobat.mobile.session.AccountSession
import app.nobat.mobile.ui.theme.ThemePrefs

class NobatApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val session: AccountSession by lazy { AccountSession(this) }
    val notificationStore: NotificationSettingsStore by lazy { NotificationSettingsStore(this) }
    val appLock: AppLockStore by lazy { AppLockStore(this) }
    val accounts: AccountRepository by lazy {
        AccountRepository(
            accounts = database.accounts(),
            appointments = database.appointments(),
            session = session,
            notificationStore = notificationStore,
        )
    }

    override fun onCreate() {
        super.onCreate()
        AppLocale.applyFromPrefs(this)
        ThemePrefs.init(this)
    }
}
