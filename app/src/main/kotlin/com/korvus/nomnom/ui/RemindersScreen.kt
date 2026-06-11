package com.korvus.nomnom.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.Reminder
import com.korvus.nomnom.notif.ReminderScheduler
import com.korvus.nomnom.ui.theme.VioletDeep
import com.korvus.nomnom.ui.theme.VioletPale
import com.korvus.nomnom.ui.theme.VioletPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(onBack: () -> Unit) {
    val app = NomNomApp.instance
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val items by app.reminderStore.items.collectAsState()

    var addingDialog by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reminder?>(null) }

    // permission launcher для POST_NOTIFICATIONS (API 33+)
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* результат игнорируем — если не дано, юзер увидит что уведомления не приходят */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Напоминания",
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = "назад",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (items.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.id }) { r ->
                        ReminderCard(
                            r = r,
                            onToggle = { enabled ->
                                scope.launch {
                                    val updated = r.copy(enabled = enabled)
                                    app.reminderStore.upsert(updated)
                                    if (enabled) ReminderScheduler.schedule(ctx, updated)
                                    else ReminderScheduler.cancel(ctx, updated)
                                }
                            },
                            onDelete = {
                                scope.launch {
                                    ReminderScheduler.cancel(ctx, r)
                                    app.reminderStore.delete(r.id)
                                }
                            },
                            onClick = { editing = r },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
            // FAB +
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .size(60.dp)
                    .clip(RoundedCornerShape(50))
                    .background(VioletPrimary)
                    .clickable {
                        ensureNotifPerm(ctx, permLauncher::launch)
                        addingDialog = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Add,
                    contentDescription = "добавить",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }

    if (addingDialog || editing != null) {
        ReminderEditDialog(
            initial = editing,
            onDismiss = { addingDialog = false; editing = null },
            onSave = { hour, minute, text, daily ->
                scope.launch {
                    val existing = editing
                    val r = existing?.copy(hour = hour, minute = minute, text = text, daily = daily)
                        ?: Reminder(
                            id = UUID.randomUUID().toString(),
                            hour = hour, minute = minute, text = text,
                            daily = daily, enabled = true,
                        )
                    app.reminderStore.upsert(r)
                    if (r.enabled) ReminderScheduler.schedule(ctx, r)
                }
                addingDialog = false
                editing = null
            },
        )
    }
}

private fun ensureNotifPerm(ctx: Context, request: (String) -> Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        request(android.Manifest.permission.POST_NOTIFICATIONS)
    }
    // exact alarm — если запрещён, попросим пользователя открыть настройки
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!am.canScheduleExactAlarms()) {
            val intent = Intent(AndroidSettings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { ctx.startActivity(intent) } catch (_: Throwable) {}
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(50))
                .background(VioletPale),
            contentAlignment = Alignment.Center,
        ) {
            Text("⏰", fontSize = 72.sp)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "Ни одного напоминания",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Тапни +, выбери время и о чём напомнить.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (r.enabled) VioletPale else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "%02d:%02d".format(r.hour, r.minute),
                color = if (r.enabled) VioletDeep else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                lineHeight = 30.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                r.text.ifBlank { "Без названия" },
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                if (r.daily) "каждый день" else "один раз",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        Switch(
            checked = r.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = VioletPrimary,
            ),
        )
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "удалить",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderEditDialog(
    initial: Reminder?,
    onDismiss: () -> Unit,
    onSave: (hour: Int, minute: Int, text: String, daily: Boolean) -> Unit,
) {
    val now = java.util.Calendar.getInstance()
    val state = rememberTimePickerState(
        initialHour = initial?.hour ?: now.get(java.util.Calendar.HOUR_OF_DAY),
        initialMinute = initial?.minute ?: 0,
        is24Hour = true,
    )
    var text by remember { mutableStateOf(initial?.text ?: "") }
    var daily by remember { mutableStateOf(initial?.daily ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) "Новое напоминание" else "Изменить",
                fontWeight = FontWeight.Black,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = state)
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Что напомнить?") },
                    placeholder = { Text("Попить воды 💧") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = false,
                    maxLines = 3,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DailyChip("Каждый день", daily) { daily = true }
                    Spacer(Modifier.width(8.dp))
                    DailyChip("Один раз", !daily) { daily = false }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(state.hour, state.minute, text.trim(), daily) },
                colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
            ) { Text("Сохранить", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = VioletPrimary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    )
}

@Composable
private fun DailyChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) VioletPrimary else MaterialTheme.colorScheme.background)
            .border(
                width = 1.dp,
                color = if (active) VioletPrimary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(
            label,
            color = if (active) Color.White else MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
