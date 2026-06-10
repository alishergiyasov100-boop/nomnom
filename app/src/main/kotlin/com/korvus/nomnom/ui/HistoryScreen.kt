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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.FoodEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val app = NomNomApp.instance
    val entries by app.dayLog.entries.collectAsState()
    val grouped = entries.groupBy { dayKey(it.timestamp) }
        .toSortedMap(compareByDescending { it })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "История",
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
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    "Тут будут дни",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Каждый день с записями станет карточкой с миниатюрами и общей калорийностью.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            grouped.forEach { (_, day) ->
                item { DayCard(day) }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

private val dayHeader = SimpleDateFormat("d MMMM", Locale("ru"))
private val dayWeek = SimpleDateFormat("EEEE", Locale("ru"))

@Composable
private fun DayCard(day: List<FoodEntry>) {
    val first = day.first()
    val totalKcal = day.sumOf { it.kcal }
    val totalP = day.sumOf { it.proteinG }
    val totalF = day.sumOf { it.fatG }
    val totalC = day.sumOf { it.carbsG }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        dayHeader.format(Date(first.timestamp)),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                    Text(
                        dayWeek.format(Date(first.timestamp))
                            .replaceFirstChar { it.titlecase(Locale("ru")) },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        totalKcal.toString(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp,
                    )
                    Text(
                        "ккал · ${day.size} ${ru("блюдо", "блюда", "блюд", day.size)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            // ряд миниатюр блюд
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                day.take(5).forEach { e ->
                    DayThumb(e, Modifier.weight(1f))
                }
                repeat((5 - day.size).coerceAtLeast(0)) {
                    Box(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MacroPill("Б", totalP, MaterialTheme.colorScheme.tertiary)
                Spacer(Modifier.width(6.dp))
                MacroPill("Ж", totalF, MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(6.dp))
                MacroPill("У", totalC, MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun DayThumb(e: FoodEntry, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(74.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.BottomStart,
    ) {
        val p = e.imagePath
        if (p != null && File(p).exists()) {
            AsyncImage(
                model = File(p),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("🍽", fontSize = 24.sp)
            }
        }
        Text(
            "${e.kcal}",
            color = androidx.compose.ui.graphics.Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
            modifier = Modifier
                .padding(5.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun MacroPill(label: String, value: Int, color: androidx.compose.ui.graphics.Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Box(
            Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            "$label ${value}г",
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
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

private fun dayKey(timestamp: Long): Long {
    val cal = Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis
}
