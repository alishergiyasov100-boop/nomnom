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
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.FoodEntry
import com.korvus.nomnom.data.startOfDayMillis
import com.korvus.nomnom.ui.theme.CarbMint
import com.korvus.nomnom.ui.theme.FatButter
import com.korvus.nomnom.ui.theme.ProteinCoral
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onCapture: () -> Unit) {
    val app = NomNomApp.instance
    val entries by app.dayLog.entries.collectAsState()
    val target by app.settings.dailyTarget.collectAsStateWithLifecycle(2000)

    val startDay = startOfDayMillis()
    val today = entries.filter { it.timestamp >= startDay }
    val todayKcal = today.sumOf { it.kcal }
    val proteinG = today.sumOf { it.proteinG }
    val fatG = today.sumOf { it.fatG }
    val carbG = today.sumOf { it.carbsG }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            greetingHello(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp,
                        )
                        Text(
                            "NomNom",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
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
                onClick = onCapture,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(10.dp))
                Text("Сфоткать", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // hero — кольцо прогресса
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    KcalRing(todayKcal, target, size = 260.dp, stroke = 18.dp)
                }
            }

            // bento — три макро
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroCard("Белки",   proteinG, suggestProteinTarget(target), ProteinCoral, Modifier.weight(1f))
                    MacroCard("Жиры",    fatG,     suggestFatTarget(target),     FatButter,    Modifier.weight(1f))
                    MacroCard("Углеводы",carbG,    suggestCarbTarget(target),    CarbMint,     Modifier.weight(1f))
                }
            }

            // section header
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Сегодня",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    Spacer(Modifier.weight(1f))
                    if (today.isNotEmpty()) {
                        Text(
                            "${today.size} ${ru("блюдо", "блюда", "блюд", today.size)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            if (today.isEmpty()) {
                item { EmptyState() }
            } else {
                items(today, key = { it.id }) { e -> MealCard(e) }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

private fun greetingHello(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..10 -> "ДОБРОЕ УТРО"
        in 11..16 -> "ДОБРЫЙ ДЕНЬ"
        in 17..21 -> "ДОБРЫЙ ВЕЧЕР"
        else -> "НОЧНОЙ ДОЗОР"
    }
}

private fun suggestProteinTarget(daily: Int) = (daily * 0.20 / 4).toInt() // 20% от ккал
private fun suggestFatTarget(daily: Int)     = (daily * 0.30 / 9).toInt() // 30%
private fun suggestCarbTarget(daily: Int)    = (daily * 0.50 / 4).toInt() // 50%

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
private fun MacroCard(label: String, value: Int, target: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                label,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.8.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value.toString(),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                )
                Text(
                    " / ${target}г",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 3.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(6.dp)) {
                MacroBar(value, target, color)
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Пока пусто",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Тапни «Сфоткать» — Qwen Vision соберёт калории и БЖУ.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

@Composable
private fun MealCard(e: FoodEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThumbBox(e.imagePath, kcal = e.kcal)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    e.dish,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    maxLines = 2,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    "${timeFmt.format(Date(e.timestamp))}  ·  Б ${e.proteinG} · Ж ${e.fatG} · У ${e.carbsG}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    e.kcal.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
                Text(
                    "ккал",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

@Composable
private fun ThumbBox(path: String?, kcal: Int) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (path != null && File(path).exists()) {
            AsyncImage(
                model = File(path),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                "🍽",
                fontSize = 26.sp,
            )
        }
    }
}
