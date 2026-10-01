package app.nobat.mobile.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.data.Appointment
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: NobatApp,
    vm: HomeViewModel = viewModel(factory = HomeViewModel.Factory(app.database.appointments())),
) {
    val day by vm.day.collectAsState()
    val rows by vm.appointments.collectAsState()
    var showBook by remember { mutableStateOf(false) }
    var pendingCancel by remember { mutableStateOf<Appointment?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val bookedMsg = stringResource(R.string.booked_toast)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBook = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_appointment))
            }
        },
    ) { padding ->
        Column(
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
        IconButton(onClick = onPrev) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.prev_day),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onToday) { Text(stringResource(R.string.today)) }
        }
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
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
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(text = a.initials, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${a.durationMin} ${stringResource(R.string.minutes_suffix)}",
                    style = MaterialTheme.typography.bodySmall,
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
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = "%02d".format(minute),
                        onValueChange = { v -> v.toIntOrNull()?.let { minute = it.coerceIn(0, 59) } },
                        label = { Text(stringResource(R.string.minute)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                OutlinedTextField(
                    value = duration.toString(),
                    onValueChange = { v -> v.toIntOrNull()?.let { duration = it.coerceIn(15, 480) } },
                    label = { Text(stringResource(R.string.duration)) },
                    singleLine = true,
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
