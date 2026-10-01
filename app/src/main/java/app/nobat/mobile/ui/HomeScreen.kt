package app.nobat.mobile.ui

import android.widget.Toast
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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.locale.AppLocale
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as DateTextStyle
import java.util.Locale
import kotlinx.coroutines.launch

private enum class AppScreen { Month, Day, Account }

private val LtrTextStyle: TextStyle
    @Composable get() = TextStyle(textDirection = TextDirection.Ltr)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: NobatApp,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.database.appointments())),
) {
    val day by vm.day.collectAsState()
    val month by vm.month.collectAsState()
    val rows by vm.appointments.collectAsState()
    val monthCounts by vm.monthCounts.collectAsState()
    var screen by remember { mutableStateOf(AppScreen.Month) }
    var showBook by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bookedMsg = stringResource(R.string.booked_toast)
    val context = LocalContext.current

    Scaffold(
        topBar = {
            when (screen) {
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
        when (screen) {
            AppScreen.Month -> MonthCalendarPane(
                month = month,
                counts = monthCounts,
                selected = day,
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth,
                onToday = {
                    vm.goToday()
                },
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
                            AppointmentCard(a) { pendingCancel = a }
                        }
                    }
                }
            }
            AppScreen.Account -> AccountPane(
                onLanguage = { showLanguage = true },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
        }
    }

    if (showBook) {
        BookDialog(
            onDismiss = { showBook = false },
            onSave = { initials, start, duration, note ->
                vm.book(initials, start, duration, note)
                showBook = false
                scope.launch { snackbar.showSnackbar(bookedMsg) }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun topBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun AccountPane(
    onLanguage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val soon = stringResource(R.string.coming_soon)
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        AccountRow(
            icon = {
                Icon(Icons.Outlined.Language, contentDescription = null)
            },
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
                Icon(Icons.Outlined.Palette, contentDescription = null)
            },
            title = stringResource(R.string.appearance),
            subtitle = soon,
            onClick = {
                Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
            },
            enabled = true,
        )
        HorizontalDivider()
        AccountRow(
            icon = {
                Icon(Icons.Outlined.DarkMode, contentDescription = null)
            },
            title = stringResource(R.string.theme),
            subtitle = soon,
            onClick = {
                Toast.makeText(context, soon, Toast.LENGTH_SHORT).show()
            },
            enabled = true,
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
    month: YearMonth,
    counts: Map<String, Int>,
    selected: LocalDate,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = LocalDate.now()
    val label = month.format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))
    val cells = remember(month) { monthGrid(month) }
    // Sat-first week labels to match Iranian PWA habit; still Gregorian dates.
    val weekDays = remember {
        listOf(
            DayOfWeek.SATURDAY,
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
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
            weekDays.forEach { dow ->
                Text(
                    text = dow.getDisplayName(DateTextStyle.NARROW, Locale.getDefault()),
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
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable { onDayClick(date) }
                                    .padding(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = date.dayOfMonth.toString(),
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

/** Gregorian month grid, Saturday-first (matches PWA week start). */
private fun monthGrid(month: YearMonth): List<LocalDate?> {
    val first = month.atDay(1)
    // DayOfWeek: MON=1 … SUN=7. We want Sat=0 … Fri=6.
    val satIndex = (first.dayOfWeek.value % 7) // Sun=0 in ISO%7? MON=1→1, … SAT=6→6, SUN=7→0
    // We want Saturday as column 0: Sat→0, Sun→1, Mon→2, … Fri→6
    val lead = when (first.dayOfWeek) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
    }
    val days = month.lengthOfMonth()
    val cells = MutableList<LocalDate?>(lead) { null }
    for (d in 1..days) cells.add(month.atDay(d))
    while (cells.size % 7 != 0) cells.add(null)
    return cells
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
    val label = day.format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US))
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // AutoMirrored Left/Right flip with LayoutDirection — do not hard-swap icons.
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
private fun AppointmentCard(a: Appointment, onCancel: () -> Unit) {
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
            OutlinedButton(onClick = onCancel) {
                Text(stringResource(R.string.cancel_appointment))
            }
        }
    }
}

@Composable
private fun BookDialog(
    onDismiss: () -> Unit,
    onSave: (initials: String, startMinute: Int, durationMin: Int, note: String) -> Unit,
) {
    val defaults = remember { nextHalfHour() }
    var initials by remember { mutableStateOf("") }
    var hour by remember { mutableIntStateOf(defaults.first) }
    var minute by remember { mutableIntStateOf(defaults.second) }
    var duration by remember { mutableIntStateOf(60) }
    var note by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val ltrFieldStyle = MaterialTheme.typography.bodyLarge.merge(LtrTextStyle)

    LaunchedEffect(Unit) { focus.requestFocus() }

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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(initials, hour * 60 + minute, duration, note) },
                enabled = initials.isNotBlank(),
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
