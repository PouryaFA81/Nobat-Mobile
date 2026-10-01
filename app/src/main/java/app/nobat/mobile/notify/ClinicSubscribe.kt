package app.nobat.mobile.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import app.nobat.mobile.NobatApp
import app.nobat.mobile.digest.EveningDigestScheduler
import app.nobat.mobile.data.AccountRole
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Staff subscribe: WorkManager periodic poll + in-process long-poll while the app is alive.
 * Admin phones publish only (Phase 1 still shows local notify on the booking device).
 * Doze may delay WorkManager — prefer always-on tablets or FCM upstream on the relay.
 */
object ClinicSubscribe {
    private const val UNIQUE_PERIODIC = "clinic_relay_poll"
    private val liveMutex = Mutex()
    private var liveJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val liveRunning = AtomicBoolean(false)

    fun ensureScheduled(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<ClinicPollWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            UNIQUE_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancelScheduled(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(UNIQUE_PERIODIC)
    }

    /** Start/restart in-process long-poll for connected Staff accounts. */
    fun startLive(context: Context) {
        val appCtx = context.applicationContext
        ensureScheduled(appCtx)
        if (!liveRunning.compareAndSet(false, true)) return
        liveJob = scope.launch {
            try {
                while (isActive) {
                    val nobat = appCtx as? NobatApp
                    if (nobat == null) {
                        delay(5_000)
                        continue
                    }
                    val accounts = nobat.database.accounts().listAll()
                    var anyConnected = false
                    for (account in accounts) {
                        if (!AccountRole.isStaff(account.role)) continue
                        val settings = nobat.clinicStore.load(account.id)
                        if (!settings.connected || !settings.isConfigured()) continue
                        anyConnected = true
                        val filterId = account.linkedPersonnelId
                        ingestOnce(appCtx, account.id, settings, filterId, longPoll = true)
                    }
                    if (!anyConnected) {
                        delay(10_000)
                    } else {
                        delay(400)
                    }
                }
            } finally {
                liveRunning.set(false)
            }
        }
    }

    fun restartLive(context: Context) {
        stopLive()
        startLive(context)
    }

    fun stopLive() {
        liveJob?.cancel()
        liveJob = null
        liveRunning.set(false)
    }

    suspend fun ingestOnce(
        context: Context,
        accountId: Long,
        settings: ClinicSettings,
        filterPersonnelId: Long,
        longPoll: Boolean,
    ) {
        liveMutex.withLock {
            val app = context.applicationContext as? NobatApp ?: return
            val store = app.clinicStore
            val current = store.load(accountId)
            if (!current.connected || !current.isConfigured()) return
            val result = if (longPoll) {
                ClinicRelayClient.longPoll(current, current.lastMessageId)
            } else {
                ClinicRelayClient.poll(current, current.lastMessageId)
            }
            val messages = result.getOrNull() ?: return
            var lastId = current.lastMessageId
            for (msg in messages) {
                if (msg.id.isNotBlank()) lastId = msg.id
                val matches = filterPersonnelId <= 0L ||
                    msg.personnelId <= 0L ||
                    msg.personnelId == filterPersonnelId
                if (!matches) continue
                when (msg.event) {
                    "book" -> ClinicNotifier.notifyBooked(
                        context,
                        msg.initials.ifBlank { msg.title },
                        msg.day,
                        msg.time,
                    )
                    "cancel" -> ClinicNotifier.notifyCancelled(
                        context,
                        msg.initials.ifBlank { msg.title },
                        msg.day,
                        msg.time,
                    )
                    "move" -> ClinicNotifier.notifyMoved(
                        context,
                        msg.initials.ifBlank { msg.title },
                        msg.day,
                        msg.time,
                    )
                    "digest" -> {
                        val count = msg.initials.toIntOrNull() ?: 0
                        ClinicNotifier.notifyDigest(context, msg.day, count)
                    }
                }
            }
            if (lastId != current.lastMessageId && lastId.isNotBlank()) {
                store.save(accountId, current.copy(lastMessageId = lastId))
            }
        }
    }
}

class ClinicPollWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? NobatApp ?: return Result.success()
        val accounts = app.database.accounts().listAll()
        for (account in accounts) {
            if (!AccountRole.isStaff(account.role)) continue
            val settings = app.clinicStore.load(account.id)
            if (!settings.connected || !settings.isConfigured()) continue
            ClinicSubscribe.ingestOnce(
                applicationContext,
                account.id,
                settings,
                account.linkedPersonnelId,
                longPoll = false,
            )
        }
        return Result.success()
    }
}

/** Re-enqueue periodic poll after boot (Doze still applies). */
class ClinicBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        ClinicSubscribe.ensureScheduled(context)
        ClinicSubscribe.startLive(context)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                EveningDigestScheduler.ensureAll(context)
            } finally {
                pending.finish()
            }
        }
    }
}
