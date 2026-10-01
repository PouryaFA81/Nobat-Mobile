package app.nobat.mobile

import android.app.Application
import app.nobat.mobile.data.AccountRepository
import app.nobat.mobile.data.AppDatabase
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.notify.ClinicSettingsStore
import app.nobat.mobile.digest.EveningDigestScheduler
import app.nobat.mobile.notify.ClinicSubscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import app.nobat.mobile.notify.TelegramSettingsStore
import app.nobat.mobile.security.AppLockStore
import app.nobat.mobile.session.AccountSession
import app.nobat.mobile.ui.theme.ThemePrefs

class NobatApp : Application() {
    val database: AppDatabase by lazy { AppDatabase.get(this) }
    val session: AccountSession by lazy { AccountSession(this) }
    val notificationStore: NotificationSettingsStore by lazy { NotificationSettingsStore(this) }
    val telegramStore: TelegramSettingsStore by lazy { TelegramSettingsStore(this) }
    val clinicStore: ClinicSettingsStore by lazy { ClinicSettingsStore(this) }
    val appLock: AppLockStore by lazy { AppLockStore(this) }
    val accounts: AccountRepository by lazy {
        AccountRepository(
            accounts = database.accounts(),
            appointments = database.appointments(),
            personnel = database.personnel(),
            session = session,
        )
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        AppLocale.applyFromPrefs(this)
        ThemePrefs.init(this)
        ClinicSubscribe.startLive(this)
        appScope.launch { EveningDigestScheduler.ensureAll(this@NobatApp) }
    }
}
