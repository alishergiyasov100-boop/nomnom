package com.korvus.nomnom.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.FoodEntry
import com.korvus.nomnom.data.startOfDayMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val app = NomNomApp.instance
    val entries by app.dayLog.entries.collectAsState()
    val target by app.settings.dailyTarget.collectAsStateWithLifecycle(2000)

    val startDay = startOfDayMillis()
    val today = entries.filter { it.timestamp >= startDay }
    val todayKcal = today.sumOf { it.kcal }
    val pct = (todayKcal.toFloat() / target).coerceIn(0f, 1.5f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🍴 NomNom",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                    )
                },
                actions = {
                    IconButton(onClick = { nav.navigate("history") }) {
                        Icon(Icons.Default.History, contentDescription = "история")
                    }
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "настройки")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { nav.navigate("capture") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Снять блюдо", fontWeight = FontWeight.SemiBold)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { TodayCard(todayKcal, target, pct, today.size) }
            item {
                Text(
                    "Сегодня съел",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }
            if (today.isEmpty()) {
                item { EmptyTodayHint() }
            } else {
                items(today, key = { it.id }) { e -> EntryRow(e) }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TodayCard(kcal: Int, target: Int, pct: Float, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Сегодня",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kcal.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "ккал",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "/ $target",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { (pct).coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.25f),
            )
            Spacer(Modifier.height(10.dp))
            val hint = when {
                count == 0 -> "Ещё ничего не ел сегодня — нажми «Снять блюдо»"
                pct < 0.4f -> "Лёгкий старт — $count ${ru("блюдо", "блюда", "блюд", count)}"
                pct < 0.9f -> "Двигаешься ровно, $count ${ru("блюдо", "блюда", "блюд", count)}"
                pct < 1f   -> "Финишная прямая, $count ${ru("блюдо", "блюда", "блюд", count)}"
                pct < 1.2f -> "В норме, можно тормозить 🌿"
                else       -> "Сегодня — банкет 🔥"
            }
            Text(
                hint,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                fontSize = 13.sp,
            )
        }
    }
}

private fun ru(one: String, few: String, many: String, n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

@Composable
private fun EmptyTodayHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Пока пусто 🍽️",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Сфоткай тарелку — и Мика-нейронка скажет калории и комментарий.",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                fontSize = 13.sp,
            )
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun EntryRow(e: FoodEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KcalBadge(e.kcal)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    e.dish,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Б ${e.proteinG} · Ж ${e.fatG} · У ${e.carbsG}   ·   ${timeFmt.format(Date(e.timestamp))}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun KcalBadge(kcal: Int) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                kcal.toString(),
                color = Color.White,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
            )
            Text(
                "ккал",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 9.sp,
            )
        }
    }
}
