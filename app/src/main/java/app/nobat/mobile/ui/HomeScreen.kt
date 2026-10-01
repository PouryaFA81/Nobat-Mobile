package app.nobat.mobile.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nobat.mobile.BuildConfig
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.data.Account
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.Personnel
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.calendar.Jalali
import app.nobat.mobile.ui.theme.ThemePrefs
import app.nobat.mobile.notify.SmsIntent
import androidx.compose.runtime.DisposableEffect
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.IntegrationInstructions
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Security
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import app.nobat.mobile.ui.account.NotificationsPane
import app.nobat.mobile.ui.account.PersonnelPane
import app.nobat.mobile.ui.security.SecurityPane
import app.nobat.mobile.ui.security.UnlockGatePane
import app.nobat.mobile.ui.shell.BackupPane
import app.nobat.mobile.ui.shell.IntegrationsPane
import app.nobat.mobile.ui.shell.ReportsPane
import app.nobat.mobile.update.UpdateChecker
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private enum class AppScreen {
    Entry,
    SignIn,
    CreateAccount,
    Unlock,
    Month,
    Day,
    Account,
    Notifications,
    Personnel,
    About,
    Security,
    Integrations,
    Backup,
    Reports,
}

private fun AppScreen.isSensitive(): Boolean = this in setOf(
    AppScreen.Month,
    AppScreen.Day,
    AppScreen.Account,
    AppScreen.Notifications,
    AppScreen.Personnel,
    AppScreen.About,
    AppScreen.Security,
    AppScreen.Integrations,
    AppScreen.Backup,
    AppScreen.Reports,
)

private fun AppScreen.isAuthFlow(): Boolean = this in setOf(
    AppScreen.Entry,
    AppScreen.SignIn,
    AppScreen.CreateAccount,
)

private val LtrTextStyle: TextStyle
    @Composable get() = TextStyle(textDirection = TextDirection.Ltr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: NobatApp,
    vm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app = app,
            dao = app.database.appointments(),
            personnelDao = app.database.personnel(),
            accountRepo = app.accounts,
            session = app.session,
            notificationStore = app.notificationStore,
        ),
    ),
) {
    val day by vm.day.collectAsState()
    val monthAnchor by vm.monthAnchor.collectAsState()
    val rows by vm.appointments.collectAsState()
    val monthCounts by vm.monthCounts.collectAsState()
    val accounts by vm.accounts.collectAsState()
    val unlockedId by vm.unlockedAccountId.collectAsState()
    val unlockedAccount by vm.unlockedAccount.collectAsState()
    val personnel by vm.personnel.collectAsState()
    val needsOrphanMigration by vm.needsOrphanMigration.collectAsState()
    val bootReady by vm.bootReady.collectAsState()
    val appLock = app.appLock
    val appLockUnlocked by appLock.unlocked.collectAsState()

    var screen by remember { mutableStateOf(AppScreen.Entry) }
    var postUnlockScreen by remember { mutableStateOf(AppScreen.Month) }
    var signInTarget by remember { mutableStateOf<Account?>(null) }
    var createAttachOrphans by remember { mutableStateOf(false) }
    var showBook by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var showChangePassword by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }
    var pendingReset by remember { mutableStateOf<Account?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bookedMsg = stringResource(R.string.booked_toast)
    val reminderScheduledMsg = stringResource(R.string.reminder_scheduled)
    val confirmationSentMsg = stringResource(R.string.confirmation_sent)
    val confirmationFailedMsg = stringResource(R.string.confirmation_failed)
    val setupSmtpMsg = stringResource(R.string.setup_smtp_first)
    val setRecipientMsg = stringResource(R.string.set_recipient)
    val addPersonnelFirstMsg = stringResource(R.string.add_personnel_first)
    var showTheme by remember { mutableStateOf(false) }
    val passwordChangedMsg = stringResource(R.string.password_changed)
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.setUseJalali(AppLocale.isPersian(context))
    }
    val config = androidx.compose.ui.platform.LocalConfiguration.current
    LaunchedEffect(config) {
        vm.setUseJalali(AppLocale.isPersian(context))
    }

    // Cold start: always Entry (or Create when orphan migration / empty).
    LaunchedEffect(bootReady, needsOrphanMigration, accounts) {
        if (!bootReady) return@LaunchedEffect
        if (unlockedId != null) return@LaunchedEffect
        if (needsOrphanMigration) {
            createAttachOrphans = true
            screen = AppScreen.CreateAccount
        } else if (accounts.isEmpty() && screen == AppScreen.Entry) {
            // Stay on Entry; empty state shows Create CTA.
        }
    }

    // If account session locks (switch), return to Entry.
    LaunchedEffect(unlockedId) {
        if (unlockedId == null && (screen.isSensitive() || screen == AppScreen.Unlock)) {
            screen = AppScreen.Entry
            signInTarget = null
        }
    }

    // Background → require app-lock unlock again when enabled.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && appLock.isLockEnabled()) {
                appLock.lockSession()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Gate sensitive UI when app lock is on and session is not unlocked.
    LaunchedEffect(unlockedId, appLockUnlocked, screen) {
        if (unlockedId == null) return@LaunchedEffect
        if (!appLock.isLockEnabled()) return@LaunchedEffect
        if (appLockUnlocked) return@LaunchedEffect
        if (screen.isSensitive()) {
            postUnlockScreen = screen
            screen = AppScreen.Unlock
        }
    }

    fun goAfterAccountAuth() {
        if (appLock.requiresUnlock()) {
            postUnlockScreen = AppScreen.Month
            screen = AppScreen.Unlock
        } else {
            if (appLock.isLockEnabled()) appLock.markUnlocked()
            screen = AppScreen.Month
        }
        signInTarget = null
    }

    Scaffold(
        topBar = {
            when (screen) {
                AppScreen.Entry -> TopAppBar(
                    title = { Text(stringResource(R.string.brand_title)) },
                    colors = topBarColors(),
                )
                AppScreen.SignIn -> TopAppBar(
                    title = { Text(stringResource(R.string.sign_in)) },
                    navigationIcon = {
                        IconButton(onClick = {
                            screen = AppScreen.Entry
                            signInTarget = null
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.CreateAccount -> TopAppBar(
                    title = { Text(stringResource(R.string.create_account)) },
                    navigationIcon = {
                        if (!needsOrphanMigration) {
                            IconButton(onClick = { screen = AppScreen.Entry }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(R.string.back),
                                )
                            }
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Month -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_calendar)) },
                    actions = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = stringResource(R.string.account_title),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Day -> TopAppBar(
                    title = { Text(stringResource(R.string.nav_calendar)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Month }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = stringResource(R.string.account_title),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Account -> TopAppBar(
                    title = { Text(stringResource(R.string.account_title)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Month }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Notifications -> TopAppBar(
                    title = { Text(stringResource(R.string.notifications_title)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Personnel -> TopAppBar(
                    title = { Text(stringResource(R.string.personnel_title)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.About -> TopAppBar(
                    title = { Text(stringResource(R.string.about)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Unlock -> TopAppBar(
                    title = { Text(stringResource(R.string.unlock)) },
                    colors = topBarColors(),
                )
                AppScreen.Security -> TopAppBar(
                    title = { Text(stringResource(R.string.security)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Integrations -> TopAppBar(
                    title = { Text(stringResource(R.string.integrations)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Backup -> TopAppBar(
                    title = { Text(stringResource(R.string.backup)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
                AppScreen.Reports -> TopAppBar(
                    title = { Text(stringResource(R.string.reports_print)) },
                    navigationIcon = {
                        IconButton(onClick = { screen = AppScreen.Account }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                            )
                        }
                    },
                    colors = topBarColors(),
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            if (screen == AppScreen.Month || screen == AppScreen.Day) {
                FloatingActionButton(
                    onClick = {
                        if (screen == AppScreen.Month) {
                            vm.selectDay(LocalDate.now())
                            screen = AppScreen.Day
                        }
                        showBook = true
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.nav_add),
                    )
                }
            }
        },
    ) { padding ->
        if (!bootReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else when (screen) {
            AppScreen.Entry -> EntryPane(
                accounts = accounts,
                lastAccountId = vm.lastAccountId(),
                onSelect = { account ->
                    signInTarget = account
                    screen = AppScreen.SignIn
                },
                onAddAccount = {
                    createAttachOrphans = false
                    screen = AppScreen.CreateAccount
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            )
            AppScreen.SignIn -> {
                val target = signInTarget
                if (target == null) {
                    LaunchedEffect(Unit) { screen = AppScreen.Entry }
                } else {
                    SignInPane(
                        account = target,
                        onSignIn = { password ->
                            val ok = vm.signIn(target.id, password)
                            if (ok) goAfterAccountAuth()
                            ok
                        },
                        onForgotReset = { pendingReset = target },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 24.dp),
                    )
                }
            }
            AppScreen.CreateAccount -> CreateAccountPane(
                attachOrphans = createAttachOrphans || needsOrphanMigration,
                onCreate = { name, password ->
                    val ok = vm.createAccount(
                        displayName = name,
                        password = password,
                        attachOrphans = createAttachOrphans || needsOrphanMigration,
                    )
                    if (ok) {
                        createAttachOrphans = false
                        goAfterAccountAuth()
                    }
                    ok
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp),
            )
            AppScreen.Month -> MonthCalendarPane(
                monthAnchor = monthAnchor,
                useJalali = AppLocale.isPersian(context),
                counts = monthCounts,
                selected = day,
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth,
                onToday = { vm.goToday() },
                onDayClick = { d ->
                    vm.selectDay(d)
                    screen = AppScreen.Day
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            )
            AppScreen.Day -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
            ) {
                DayBar(
                    day = day,
                    onPrev = vm::prevDay,
                    onNext = vm::nextDay,
                    onToday = vm::goToday,
                )
                Spacer(Modifier.height(8.dp))
                if (rows.isEmpty()) {
                    EmptyDayCard(onAdd = { showBook = true })
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(rows, key = { it.id }) { a ->
                            AppointmentCard(
                                a = a,
                                onCancel = { pendingCancel = a },
                                onSms = {
                                    val time = formatTime(a.startMinute)
                                    val body = SmsIntent.reminderBody(
                                        initials = a.initials,
                                        date = day.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        time = time,
                                        durationMin = a.durationMin,
                                        minutesSuffix = context.getString(R.string.minutes_suffix),
                                        template = context.getString(R.string.sms_reminder_sample),
                                    )
                                    SmsIntent.openComposer(context, body)
                                },
                            )
                        }
                    }
                }
            }
            AppScreen.Account -> AccountPane(
                displayName = unlockedAccount?.displayName,
                onPassword = { showChangePassword = true },
                onSwitch = {
                    vm.switchAccount()
                    screen = AppScreen.Entry
                },
                onAddAccount = {
                    vm.switchAccount()
                    createAttachOrphans = false
                    screen = AppScreen.CreateAccount
                },
                onLanguage = { showLanguage = true },
                onTheme = { showTheme = true },
                onNotifications = { screen = AppScreen.Notifications },
                onPersonnel = { screen = AppScreen.Personnel },
                onSecurity = { screen = AppScreen.Security },
                onIntegrations = { screen = AppScreen.Integrations },
                onBackup = { screen = AppScreen.Backup },
                onReports = { screen = AppScreen.Reports },
                onAbout = { screen = AppScreen.About },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Notifications -> {
                val aid = unlockedId
                if (aid == null) {
                    LaunchedEffect(Unit) { screen = AppScreen.Entry }
                } else {
                    NotificationsPane(
                        accountId = aid,
                        store = app.notificationStore,
                        snackbar = snackbar,
                        onSettingsSaved = { vm.syncRemindersForAccount(aid) },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                    )
                }
            }
            AppScreen.Personnel -> PersonnelPane(
                people = personnel,
                onSave = { id, name, email, phone -> vm.upsertPersonnel(id, name, email, phone) },
                onDelete = { id -> vm.deletePersonnel(id) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.About -> AboutPane(
                snackbar = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Unlock -> UnlockGatePane(
                store = appLock,
                onUnlocked = {
                    screen = if (postUnlockScreen.isSensitive()) postUnlockScreen else AppScreen.Month
                },
                onUseAccount = {
                    vm.switchAccount()
                    screen = AppScreen.Entry
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Security -> SecurityPane(
                store = appLock,
                snackbar = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Integrations -> IntegrationsPane(
                snackbar = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Backup -> BackupPane(
                snackbar = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            AppScreen.Reports -> ReportsPane(
                snackbar = snackbar,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    if (showBook) {
        BookDialog(
            personnel = personnel,
            onDismiss = { showBook = false },
            onNeedPersonnel = {
                showBook = false
                scope.launch {
                    snackbar.showSnackbar(addPersonnelFirstMsg)
                }
                screen = AppScreen.Personnel
            },
            onSave = { initials, start, duration, note, personnelId ->
                scope.launch {
                    val result = vm.book(initials, start, duration, note, personnelId)
                    showBook = false
                    when (result) {
                        is HomeViewModel.BookResult.Ok -> {
                            snackbar.showSnackbar(bookedMsg)
                            if (result.reminderScheduled) {
                                snackbar.showSnackbar(reminderScheduledMsg)
                            }
                            when (result.confirmationSent) {
                                true -> snackbar.showSnackbar(confirmationSentMsg)
                                false -> snackbar.showSnackbar(confirmationFailedMsg)
                                null -> {}
                            }
                            if (result.needSmtp) {
                                snackbar.showSnackbar(setupSmtpMsg)
                            }
                        }
                        HomeViewModel.BookResult.NeedPersonnel -> {
                            snackbar.showSnackbar(addPersonnelFirstMsg)
                            screen = AppScreen.Personnel
                        }
                        HomeViewModel.BookResult.Failed -> {}
                    }
                }
            },
        )
    }

    if (showTheme) {
        ThemeDialog(
            dark = ThemePrefs.isDark(context),
            onDismiss = { showTheme = false },
            onSelect = { dark ->
                ThemePrefs.setDark(context, dark)
                showTheme = false
            },
        )
    }

    if (showLanguage) {
        LanguageDialog(
            current = AppLocale.getPreference(context),
            onDismiss = { showLanguage = false },
            onSelect = { tag ->
                showLanguage = false
                AppLocale.setPreference(context, tag)
            },
        )
    }

    if (showChangePassword) {
        ChangePasswordDialog(
            onDismiss = { showChangePassword = false },
            onSave = { current, newPass ->
                val ok = vm.changePassword(current, newPass)
                if (ok) {
                    showChangePassword = false
                    scope.launch { snackbar.showSnackbar(passwordChangedMsg) }
                }
                ok
            },
        )
    }

    pendingCancel?.let { appt ->
        AlertDialog(
            onDismissRequest = { pendingCancel = null },
            title = { Text(stringResource(R.string.cancel_appointment)) },
            text = { Text(stringResource(R.string.confirm_cancel)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancel(appt.id)
                    pendingCancel = null
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancel = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }

    pendingReset?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingReset = null },
            title = { Text(stringResource(R.string.reset_account)) },
            text = { Text(stringResource(R.string.confirm_reset_account)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        vm.resetAccount(account.id)
                        pendingReset = null
                        signInTarget = null
                        screen = AppScreen.Entry
                    }
                }) { Text(stringResource(R.string.yes)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingReset = null }) {
                    Text(stringResource(R.string.no))
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun EntryPane(
    accounts: List<Account>,
    lastAccountId: Long?,
    onSelect: (Account) -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.profiles_heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (accounts.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(R.string.no_accounts_hint),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = stringResource(R.string.local_only_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = onAddAccount,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(stringResource(R.string.create_account))
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(accounts, key = { it.id }) { account ->
                    val highlight = account.id == lastAccountId
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(account) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (highlight) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                Icons.Outlined.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp),
                            )
                            Text(
                                text = account.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onAddAccount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text(stringResource(R.string.add_account))
            }
            Text(
                text = stringResource(R.string.local_only_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SignInPane(
    account: Account,
    onSignIn: suspend (CharArray) -> Boolean,
    onForgotReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val wrong = stringResource(R.string.wrong_password)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.AccountCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = account.displayName,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.local_only_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                error = null
            },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        Button(
            onClick = {
                if (busy) return@Button
                busy = true
                scope.launch {
                    val chars = password.toCharArray()
                    val ok = onSignIn(chars)
                    if (!ok) error = wrong
                    password = ""
                    busy = false
                }
            },
            enabled = password.isNotEmpty() && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.sign_in))
        }
        TextButton(onClick = onForgotReset) {
            Text(
                text = stringResource(R.string.forgot_reset_note),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CreateAccountPane(
    attachOrphans: Boolean,
    onCreate: suspend (String, CharArray) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val mismatch = stringResource(R.string.password_mismatch)
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (attachOrphans) {
            Text(
                text = stringResource(R.string.orphan_migration_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = stringResource(R.string.orphan_migration_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(R.string.local_only_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; error = null },
            label = { Text(stringResource(R.string.display_name)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focus),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; error = null },
            label = { Text(stringResource(R.string.password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it; error = null },
            label = { Text(stringResource(R.string.confirm_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = {
                if (busy) return@Button
                if (password != confirm) {
                    error = mismatch
                    return@Button
                }
                busy = true
                scope.launch {
                    val ok = onCreate(name, password.toCharArray())
                    if (!ok) error = mismatch
                    busy = false
                }
            },
            enabled = name.isNotBlank() && password.isNotEmpty() && confirm.isNotEmpty() && !busy,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Text(stringResource(R.string.create_account))
        }
    }
}

@Composable
private fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onSave: suspend (CharArray, CharArray) -> Boolean,
) {
    var current by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val wrong = stringResource(R.string.wrong_password)
    val mismatch = stringResource(R.string.password_mismatch)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.change_password)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = current,
                    onValueChange = { current = it; error = null },
                    label = { Text(stringResource(R.string.current_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = newPass,
                    onValueChange = { newPass = it; error = null },
                    label = { Text(stringResource(R.string.new_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = { confirm = it; error = null },
                    label = { Text(stringResource(R.string.confirm_password)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (busy) return@Button
                    if (newPass != confirm) {
                        error = mismatch
                        return@Button
                    }
                    busy = true
                    scope.launch {
                        val ok = onSave(current.toCharArray(), newPass.toCharArray())
                        if (!ok) error = wrong
                        busy = false
                    }
                },
                enabled = current.isNotEmpty() && newPass.isNotEmpty() && confirm.isNotEmpty() && !busy,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun AccountPane(
    displayName: String?,
    onPassword: () -> Unit,
    onSwitch: () -> Unit,
    onAddAccount: () -> Unit,
    onLanguage: () -> Unit,
    onTheme: () -> Unit,
    onNotifications: () -> Unit,
    onPersonnel: () -> Unit,
    onSecurity: () -> Unit,
    onIntegrations: () -> Unit,
    onBackup: () -> Unit,
    onReports: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dark = ThemePrefs.isDark(context)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        if (!displayName.isNullOrBlank()) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            HorizontalDivider()
        }

        AccountSectionHeader(stringResource(R.string.section_account_management))
        AccountRow(
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            title = stringResource(R.string.password),
            subtitle = stringResource(R.string.change_password),
            onClick = onPassword,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.SwapHoriz, contentDescription = null) },
            title = stringResource(R.string.switch_account),
            subtitle = null,
            onClick = onSwitch,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.PersonAdd, contentDescription = null) },
            title = stringResource(R.string.add_account),
            subtitle = null,
            onClick = onAddAccount,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Security, contentDescription = null) },
            title = stringResource(R.string.security),
            subtitle = stringResource(R.string.app_lock),
            onClick = onSecurity,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Groups, contentDescription = null) },
            title = stringResource(R.string.personnel_title),
            subtitle = null,
            onClick = onPersonnel,
        )

        AccountSectionHeader(stringResource(R.string.section_preferences))
        AccountRow(
            icon = { Icon(Icons.Outlined.Language, contentDescription = null) },
            title = stringResource(R.string.language_label),
            subtitle = if (AppLocale.isPersian(context)) {
                stringResource(R.string.language_fa)
            } else {
                stringResource(R.string.language_en)
            },
            onClick = onLanguage,
        )
        HorizontalDivider()
        AccountRow(
            icon = {
                Icon(
                    if (dark) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                    contentDescription = null,
                )
            },
            title = stringResource(R.string.appearance),
            subtitle = if (dark) {
                stringResource(R.string.theme_dark)
            } else {
                stringResource(R.string.theme_light)
            },
            onClick = onTheme,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Notifications, contentDescription = null) },
            title = stringResource(R.string.notifications_title),
            subtitle = null,
            onClick = onNotifications,
        )

        AccountSectionHeader(stringResource(R.string.section_advanced_settings))
        AccountRow(
            icon = { Icon(Icons.Outlined.IntegrationInstructions, contentDescription = null) },
            title = stringResource(R.string.integrations),
            subtitle = stringResource(R.string.coming_soon),
            onClick = onIntegrations,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Cloud, contentDescription = null) },
            title = stringResource(R.string.backup),
            subtitle = stringResource(R.string.coming_soon),
            onClick = onBackup,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Print, contentDescription = null) },
            title = stringResource(R.string.reports_print),
            subtitle = stringResource(R.string.coming_soon),
            onClick = onReports,
        )

        AccountSectionHeader(stringResource(R.string.section_about))
        AccountRow(
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = stringResource(R.string.about),
            subtitle = null,
            onClick = onAbout,
        )
    }
}

@Composable
private fun AccountSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
    )
}


private const val URL_BUG =
    "https://github.com/PouryaFA81/Nobat-Mobile/issues/new?labels=bug"
private const val URL_SUGGESTIONS =
    "https://github.com/PouryaFA81/Nobat-Mobile/issues/new?labels=enhancement"
private const val URL_SUPPORT =
    "https://github.com/PouryaFA81/Nobat-Mobile/issues"

@Composable
private fun AboutPane(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = BuildConfig.VERSION_NAME
    val upToDateMsg = stringResource(R.string.up_to_date)
    val updateAvailableMsg = stringResource(R.string.update_available)
    val openReleaseLabel = stringResource(R.string.open_release)
    val checkFailedMsg = stringResource(R.string.update_check_failed)
    var checking by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }

    Column(modifier = modifier.padding(vertical = 8.dp)) {
        AccountRow(
            icon = { Icon(Icons.Outlined.Info, contentDescription = null) },
            title = stringResource(R.string.version),
            subtitle = versionName,
            onClick = {},
            enabled = false,
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.SystemUpdate, contentDescription = null) },
            title = stringResource(R.string.check_for_updates),
            subtitle = if (checking) "…" else null,
            onClick = {
                if (checking) return@AccountRow
                checking = true
                scope.launch {
                    val result = UpdateChecker.check(versionName)
                    checking = false
                    if (result == null) {
                        snackbar.showSnackbar(checkFailedMsg)
                        return@launch
                    }
                    if (!result.updateAvailable) {
                        snackbar.showSnackbar(upToDateMsg)
                    } else {
                        val action = snackbar.showSnackbar(
                            message = updateAvailableMsg,
                            actionLabel = openReleaseLabel,
                            duration = SnackbarDuration.Long,
                        )
                        if (action == SnackbarResult.ActionPerformed) {
                            openUrl(result.releaseUrl)
                        }
                    }
                }
            },
            enabled = !checking,
        )
        HorizontalDivider()
        Text(
            text = stringResource(R.string.contact),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        AccountRow(
            icon = { Icon(Icons.Outlined.BugReport, contentDescription = null) },
            title = stringResource(R.string.bug_report),
            subtitle = null,
            onClick = { openUrl(URL_BUG) },
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.Lightbulb, contentDescription = null) },
            title = stringResource(R.string.suggestions),
            subtitle = null,
            onClick = { openUrl(URL_SUGGESTIONS) },
        )
        HorizontalDivider()
        AccountRow(
            icon = { Icon(Icons.Outlined.HelpOutline, contentDescription = null) },
            title = stringResource(R.string.support),
            subtitle = null,
            onClick = { openUrl(URL_SUPPORT) },
        )
    }
}

@Composable
private fun AccountRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        icon()
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthCalendarPane(
    monthAnchor: LocalDate,
    useJalali: Boolean,
    counts: Map<String, Int>,
    selected: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val label = if (useJalali) {
        Jalali.monthLabel(monthAnchor)
    } else {
        YearMonth.from(monthAnchor).format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))
    }
    val cells = remember(monthAnchor, useJalali) {
        if (useJalali) Jalali.monthGrid(monthAnchor) else monthGridGregorian(YearMonth.from(monthAnchor))
    }
    val weekHeaders: List<String> = if (useJalali) {
        Jalali.WEEK_HEADER_FA
    } else {
        listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        ).map { it.getDisplayName(DateTextStyle.NARROW, Locale.getDefault()) }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrev) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.prev_month),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium.merge(LtrTextStyle),
                )
                TextButton(onClick = onToday) { Text(stringResource(R.string.today)) }
            }
            IconButton(onClick = onNext) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.next_month),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            weekHeaders.forEach { header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (date != null) {
                            val iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            val count = counts[iso] ?: 0
                            val isToday = date == today
                            val isSelected = date == selected
                            val dayNum = if (useJalali) {
                                Jalali.dayOfMonth(date).toString()
                            } else {
                                date.dayOfMonth.toString()
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                    )
                                    .clickable { onDayClick(date) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = dayNum,
                                    style = MaterialTheme.typography.bodyMedium.merge(LtrTextStyle),
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (count > 0) {
                                    Text(
                                        text = count.toString(),
                                        style = MaterialTheme.typography.labelSmall.merge(LtrTextStyle),
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                } else {
                                    Spacer(Modifier.height(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.empty_month_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Gregorian month grid, Saturday-first (existing EN week layout). */
private fun monthGridGregorian(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    val lead = Jalali.saturdayFirstIndex(first.dayOfWeek)
    val days = month.lengthOfMonth()
    val cells = MutableList<LocalDate?>(lead) { null }
    for (d in 1..days) cells.add(month.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

@Composable
private fun ThemeDialog(
    dark: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Boolean) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.appearance)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LanguageOption(
                    label = stringResource(R.string.theme_dark),
                    selected = dark,
                    onClick = { onSelect(true) },
                )
                LanguageOption(
                    label = stringResource(R.string.theme_light),
                    selected = !dark,
                    onClick = { onSelect(false) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguageDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val context = LocalContext.current
    val effective = AppLocale.effectiveLanguage(context)
    val selected = when (current) {
        AppLocale.FA, AppLocale.EN -> current
        else -> effective
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language_label)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LanguageOption(
                    label = stringResource(R.string.language_fa),
                    selected = selected == AppLocale.FA,
                    onClick = { onSelect(AppLocale.FA) },
                )
                LanguageOption(
                    label = stringResource(R.string.language_en),
                    selected = selected == AppLocale.EN,
                    onClick = { onSelect(AppLocale.EN) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Start,
            color = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

@Composable
private fun EmptyDayCard(onAdd: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringResource(R.string.empty_day_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.empty_day_goal),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onAdd) {
                Text(stringResource(R.string.empty_day_cta))
            }
        }
    }
}

@Composable
private fun DayBar(
    day: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    val context = LocalContext.current
    val useJalali = AppLocale.isPersian(context)
    val label = if (useJalali) Jalali.dayLabel(day) else day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.prev_day),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.merge(LtrTextStyle),
            )
            TextButton(onClick = onToday) { Text(stringResource(R.string.today)) }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.next_day),
            )
        }
    }
}

@Composable
private fun AppointmentCard(
    a: Appointment,
    onCancel: () -> Unit,
    onSms: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = formatTime(a.startMinute),
                    style = MaterialTheme.typography.titleMedium.merge(LtrTextStyle),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = a.initials, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${a.durationMin} ${stringResource(R.string.minutes_suffix)}",
                    style = MaterialTheme.typography.bodySmall.merge(LtrTextStyle),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (a.note.isNotBlank()) {
                    Text(
                        text = a.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onSms) {
                    Icon(
                        Icons.Outlined.Sms,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.sms_share))
                }
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.cancel_appointment))
                }
            }
        }
    }
}

@Composable
private fun BookDialog(
    personnel: List<Personnel>,
    onDismiss: () -> Unit,
    onNeedPersonnel: () -> Unit,
    onSave: (initials: String, startMinute: Int, durationMin: Int, note: String, personnelId: Long) -> Unit,
) {
    val defaults = remember { nextHalfHour() }
    var initials by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(defaults.first) }
    var minute by remember { mutableIntStateOf(defaults.second) }
    var duration by remember { mutableIntStateOf(60) }
    var note by remember { mutableStateOf("") }
    var selectedPersonnelId by remember {
        mutableStateOf(personnel.firstOrNull()?.id ?: 0L)
    }
    val focus = remember { FocusRequester() }
    val ltrFieldStyle = MaterialTheme.typography.bodyLarge.merge(LtrTextStyle)

    LaunchedEffect(Unit) {
        if (personnel.isEmpty()) {
            onNeedPersonnel()
        } else {
            focus.requestFocus()
        }
    }

    if (personnel.isEmpty()) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.book_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = initials,
                    onValueChange = { initials = it },
                    label = { Text(stringResource(R.string.initials)) },
                    placeholder = { Text(stringResource(R.string.initials_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focus),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hour.toString(),
                        onValueChange = { v -> v.toIntOrNull()?.let { hour = it.coerceIn(0, 23) } },
                        label = { Text(stringResource(R.string.hour)) },
                        singleLine = true,
                        textStyle = ltrFieldStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = "%02d".format(minute),
                        onValueChange = { v -> v.toIntOrNull()?.let { minute = it.coerceIn(0, 59) } },
                        label = { Text(stringResource(R.string.minute)) },
                        singleLine = true,
                        textStyle = ltrFieldStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = duration.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { duration = it.coerceIn(15, 480) } },
                    label = { Text(stringResource(R.string.duration)) },
                    singleLine = true,
                    textStyle = ltrFieldStyle,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(stringResource(R.string.note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.assign_to),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    personnel.forEach { person ->
                        val selected = person.id == selectedPersonnelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPersonnelId = person.id }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = selected,
                                onClick = { selectedPersonnelId = person.id },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(person.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    text = person.email,
                                    style = MaterialTheme.typography.bodySmall.merge(LtrTextStyle),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(initials, hour * 60 + minute, duration, note, selectedPersonnelId)
                },
                enabled = initials.isNotBlank() && selectedPersonnelId > 0L,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Next :00 or :30 slot from now (rolls to next hour if past :30). */
private fun nextHalfHour(): Pair<Int, Int> {
    val t = LocalTime.now()
    return when {
        t.minute == 0 && t.second == 0 -> t.hour to 0
        t.minute < 30 -> t.hour to 30
        t.hour == 23 -> 23 to 30
        else -> (t.hour + 1) to 0
    }
}

private fun formatTime(startMinute: Int): String {
    val h = startMinute / 60
    val m = startMinute % 60
    return "%02d:%02d".format(h, m)
}
