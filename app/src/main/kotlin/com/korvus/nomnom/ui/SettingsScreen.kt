package com.korvus.nomnom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.navigation.NavController
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.Settings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val app = NomNomApp.instance
    val scope = rememberCoroutineScope()

    val baseUrlStored by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = Settings.DEFAULT_BASE_URL)
    val modelStored by app.settings.model.collectAsStateWithLifecycle(initialValue = Settings.DEFAULT_MODEL)
    val apiKeyStored by app.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")
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
                title = { Text("Настройки", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "назад")
                    }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Vision-API",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )

            OutlinedTextField(
                value = baseUrl,
                onValueChange = { baseUrl = it },
                label = { Text("Base URL (OpenAI-compat)") },
                placeholder = { Text(Settings.DEFAULT_BASE_URL) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = model,
                onValueChange = { model = it },
                label = { Text("Модель") },
                placeholder = { Text(Settings.DEFAULT_MODEL) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API key (если нужен)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(6.dp))
            Text(
                "Цель",
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = target,
                onValueChange = { target = it.filter(Char::isDigit) },
                label = { Text("ккал в день") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    scope.launch {
                        app.settings.setBaseUrl(baseUrl.trim())
                        app.settings.setModel(model.trim())
                        app.settings.setApiKey(apiKey.trim())
                        app.settings.setDailyTarget(target.toIntOrNull() ?: 2000)
                        nav.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("Сохранить", fontWeight = FontWeight.SemiBold) }

            TextButton(onClick = {
                scope.launch { app.dayLog.clearAll() }
            }) {
                Text(
                    "Очистить всю историю",
                    color = MaterialTheme.colorScheme.tertiary,
                    fontSize = 13.sp,
                )
            }
        }
    }
}
