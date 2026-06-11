package com.korvus.nomnom.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onReminders: () -> Unit = {}) {
    val app = NomNomApp.instance
    val scope = rememberCoroutineScope()

    val baseUrlStored by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = Settings.DEFAULT_BASE_URL)
    val modelStored by app.settings.model.collectAsStateWithLifecycle(initialValue = Settings.DEFAULT_MODEL)
    val apiKeyStored by app.settings.apiKey.collectAsStateWithLifecycle(initialValue = Settings.DEFAULT_API_KEY)
    val targetStored by app.settings.dailyTarget.collectAsStateWithLifecycle(initialValue = 2000)

    var baseUrl by remember { mutableStateOf(baseUrlStored) }
    var model by remember { mutableStateOf(modelStored) }
    var apiKey by remember { mutableStateOf(apiKeyStored) }
    var target by remember { mutableStateOf(targetStored.toString()) }

    LaunchedEffect(baseUrlStored) { baseUrl = baseUrlStored }
    LaunchedEffect(modelStored) { model = modelStored }
    LaunchedEffect(apiKeyStored) { apiKey = apiKeyStored }
    LaunchedEffect(targetStored) { target = targetStored.toString() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Настройки",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard(
                title = "Vision API",
                hint = "Куда отправлять фото блюд для оценки калорий. Если несколько ключей — впиши по одному в строку, NomNom будет крутить их по кругу и переключаться при лимите (429)."
            ) {
                Text(
                    "БЫСТРЫЙ ВЫБОР",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Settings.PRESETS) { p ->
                        PresetChip(
                            label = p.label,
                            active = baseUrl.trim() == p.baseUrl && model.trim() == p.model,
                            onClick = {
                                baseUrl = p.baseUrl
                                model = p.model
                            },
                        )
                    }
                }
                FieldOutlined("Base URL", baseUrl, Settings.DEFAULT_BASE_URL) { baseUrl = it }
                FieldOutlined("Модель", model, Settings.DEFAULT_MODEL) { model = it }
                FieldMultiline("API ключи (по одному в строку)", apiKey, "sk-...") { apiKey = it }
                val keyCount = Settings.parseKeys(apiKey).size
                if (keyCount > 0) {
                    Text(
                        "🔑 в ротации: $keyCount",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }
            SectionCard(title = "Цель", hint = "Ежедневная норма калорий — для прогресс-кольца.") {
                FieldOutlined("ккал в день", target, "2000") { target = it.filter(Char::isDigit) }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onReminders)
                    .padding(18.dp),
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("🔔", fontSize = 22.sp)
                    Spacer(Modifier.height(0.dp))
                    androidx.compose.foundation.layout.Spacer(Modifier.size(14.dp))
                    androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Напоминания",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            "Попить, покушать, перекусить — на твоё время.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                        )
                    }
                    Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(2.dp))
            Button(
                onClick = {
                    scope.launch {
                        app.settings.setBaseUrl(baseUrl.trim())
                        app.settings.setModel(model.trim())
                        app.settings.setApiKey(apiKey.trim())
                        app.settings.setDailyTarget(target.toIntOrNull() ?: 2000)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("Сохранить", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
            TextButton(
                onClick = { scope.launch { app.dayLog.clearAll() } },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Очистить всю историю",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SectionCard(title: String, hint: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                title,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Text(
                hint,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
            Spacer(Modifier.height(2.dp))
            content()
        }
    }
}

@Composable
private fun PresetChip(label: String, active: Boolean, onClick: () -> Unit) {
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(label, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldOutlined(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FieldMultiline(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        singleLine = false,
        minLines = 2,
        maxLines = 8,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.background,
            unfocusedContainerColor = MaterialTheme.colorScheme.background,
        ),
    )
}
