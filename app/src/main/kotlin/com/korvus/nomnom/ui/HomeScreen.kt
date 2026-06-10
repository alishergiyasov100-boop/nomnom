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
import androidx.compose.material3.ExperimentalMaterial3Api
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

@OptIn(ExperimentalMaterial3Api::class)
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
                        "NomNom",
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    IconButton(onClick = { nav.navigate("history") }) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = "история",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    IconButton(onClick = { nav.navigate("settings") }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "настройки",
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
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
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Снять блюдо", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { HeroTodayCard(todayKcal, target, pct, today.size) }
            if (today.isNotEmpty()) {
                item { MacroSummaryRow(today) }
            }
            item {
                Text(
                    "сегодня",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                )
            }
            if (today.isEmpty()) {
                item { EmptyTodayHint() }
            } else {
                items(today, key = { it.id }) { e -> EntryRow(e) }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

@Composable
private fun HeroTodayCard(kcal: Int, target: Int, pct: Float, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp)) {
            Text(
                greeting(),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    kcal.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 64.sp,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.padding(bottom = 10.dp)) {
                    Text(
                        "ккал",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "из $target",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.55f),
                        fontSize = 12.sp,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            LinearProgressIndicator(
                progress = { pct.coerceAtMost(1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.onPrimary,
                trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                hintFor(count, pct),
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                fontSize = 13.sp,
            )
        }
    }
}

private fun greeting(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..10 -> "ДОБРОЕ УТРО"
        in 11..16 -> "ДЕНЬ"
        in 17..21 -> "ВЕЧЕР"
        else -> "НОЧЬ"
    }
}

private fun hintFor(count: Int, pct: Float): String = when {
    count == 0 -> "Тарелка пуста — самое время кадрировать обед 📸"
    pct < 0.4f -> "Лёгкий старт, $count ${ru("приём", "приёма", "приёмов", count)} пищи"
    pct < 0.9f -> "В ритме — двигаешься ровно"
    pct < 1f   -> "Финишная прямая на сегодня"
    pct < 1.2f -> "Норма закрыта, можно завершать"
    else       -> "Сегодня — банкет 🔥"
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
private fun MacroSummaryRow(today: List<FoodEntry>) {
    val p = today.sumOf { it.proteinG }
    val f = today.sumOf { it.fatG }
    val c = today.sumOf { it.carbsG }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroMini("белки", p, Modifier.weight(1f))
        MacroMini("жиры", f, Modifier.weight(1f))
        MacroMini("углеводы", c, Modifier.weight(1f))
    }
}

@Composable
private fun MacroMini(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                )
                Text(
                    " г",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyTodayHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Text(
                "Здесь будут блюда",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Снимешь тарелку — Qwen Vision соберёт калории и БЖУ, я добавлю в ленту.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
fun EntryRow(e: FoodEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KcalDot(e.kcal)
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
                    "${timeFmt.format(Date(e.timestamp))}   ·   Б ${e.proteinG} · Ж ${e.fatG} · У ${e.carbsG}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun KcalDot(kcal: Int) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                kcal.toString(),
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Black,
                fontSize = 17.sp,
            )
            Text(
                "ккал",
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                fontSize = 9.sp,
            )
        }
    }
}
