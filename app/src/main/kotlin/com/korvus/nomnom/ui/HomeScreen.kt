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
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.korvus.nomnom.ui.theme.MintBg
import com.korvus.nomnom.ui.theme.MintText
import com.korvus.nomnom.ui.theme.PeachBg
import com.korvus.nomnom.ui.theme.PeachText
import com.korvus.nomnom.ui.theme.PinkRoseBg
import com.korvus.nomnom.ui.theme.PinkRoseText
import com.korvus.nomnom.ui.theme.VioletDeep
import com.korvus.nomnom.ui.theme.VioletLight
import com.korvus.nomnom.ui.theme.VioletPillBg
import com.korvus.nomnom.ui.theme.VioletPrimary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
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
    val remaining = (target - todayKcal).coerceAtLeast(0)
    val p = today.sumOf { it.proteinG }
    val f = today.sumOf { it.fatG }
    val c = today.sumOf { it.carbsG }
    val pTarget = (target * 0.20 / 4).toInt()
    val fTarget = (target * 0.30 / 9).toInt()
    val cTarget = (target * 0.50 / 4).toInt()

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item {
                HeroCard(
                    kcal = todayKcal,
                    target = target,
                    remaining = remaining,
                    protein = p, proteinTarget = pTarget,
                    fat = f, fatTarget = fTarget,
                    carbs = c, carbsTarget = cTarget,
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Приёмы пищи",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 19.sp,
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
                Spacer(Modifier.height(12.dp))
            }
            item {
                val byMeal = groupByMeal(today)
                Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MealsGrid(byMeal)
                }
            }
            if (today.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Text(
                            "Последние блюда",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Black,
                            fontSize = 17.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                }
                items(today, key = { it.id }) { e ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp)) {
                        DishRow(e)
                    }
                }
            }
            item { Spacer(Modifier.height(120.dp)) }
        }
    }
}

/* ── HERO: фиолетовый gradient, дата, 3 колонки, 3 макро-бара ── */
@Composable
private fun HeroCard(
    kcal: Int,
    target: Int,
    remaining: Int,
    protein: Int, proteinTarget: Int,
    fat: Int, fatTarget: Int,
    carbs: Int, carbsTarget: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.linearGradient(colors = listOf(VioletPrimary, VioletLight)),
                shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
            )
            .padding(horizontal = 24.dp, vertical = 22.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // верх: иконки + "Дневник"
            Row(verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.width(40.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    "Дневник",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                )
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Tune, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(14.dp))
            // дата с <  >
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Outlined.ChevronLeft,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Сегодня, ${todayLabel()}",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                )
            }
            Spacer(Modifier.height(20.dp))
            // 3 колонки: norma | ring | осталось
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatColumn(target.toString(), "норма", Modifier.weight(1f))
                HeroKcalRing(kcal = kcal, target = target, size = 120.dp, stroke = 11.dp)
                StatColumn(remaining.toString(), "осталось", Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            // 3 макро-бара
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroMacro("Белки",   protein, proteinTarget, Modifier.weight(1f))
                HeroMacro("Жиры",    fat,     fatTarget,     Modifier.weight(1f))
                HeroMacro("Углеводы",carbs,   carbsTarget,   Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatColumn(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
            lineHeight = 24.sp,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = Color.White.copy(alpha = 0.82f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun HeroMacro(label: String, value: Int, target: Int, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(6.dp)) {
            MacroBarOnHero(value, target)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "$value / $target г",
            color = Color.White.copy(alpha = 0.88f),
            fontSize = 11.sp,
        )
    }
}

private fun todayLabel(): String {
    val fmt = SimpleDateFormat("d MMMM", Locale("ru"))
    return fmt.format(Date())
}

/* ── Meal cards: Завтрак / Обед / Ужин / Перекус (только те что не пустые показываем) ── */

private data class MealBucket(
    val name: String,
    val emoji: String,
    val entries: List<FoodEntry>,
    val pillTime: String?,
)

private fun mealOf(ts: Long): String {
    val h = Calendar.getInstance().apply { timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..10 -> "Завтрак"
        in 11..15 -> "Обед"
        in 16..21 -> "Ужин"
        else -> "Перекус"
    }
}

private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun groupByMeal(today: List<FoodEntry>): List<MealBucket> {
    val emojis = mapOf(
        "Завтрак" to "🍳",
        "Обед" to "🥗",
        "Ужин" to "🍝",
        "Перекус" to "🍎",
    )
    val grouped = today.groupBy { mealOf(it.timestamp) }
    return listOf("Завтрак", "Обед", "Ужин", "Перекус").map { name ->
        val items = grouped[name].orEmpty().sortedByDescending { it.timestamp }
        MealBucket(
            name = name,
            emoji = emojis[name] ?: "🍽",
            entries = items,
            pillTime = items.firstOrNull()?.let { timeFmt.format(Date(it.timestamp)) },
        )
    }
}

@Composable
private fun MealsGrid(buckets: List<MealBucket>) {
    // 2 ряда по 2 карточки
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MealCard(buckets[0], Modifier.weight(1f))
            MealCard(buckets[1], Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MealCard(buckets[2], Modifier.weight(1f))
            MealCard(buckets[3], Modifier.weight(1f))
        }
    }
}

@Composable
private fun MealCard(b: MealBucket, modifier: Modifier = Modifier) {
    val kcal = b.entries.sumOf { it.kcal }
    val isEmpty = b.entries.isEmpty()
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    b.name,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.weight(1f))
                if (b.pillTime != null) {
                    TimePill(b.pillTime)
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(50))
                            .background(VioletPillBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+",
                            color = VioletDeep,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        if (isEmpty) "0" else kcal.toString(),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Black,
                        fontSize = 22.sp,
                    )
                    Text(
                        "ккал",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                    )
                }
                Text(b.emoji, fontSize = 28.sp)
            }
        }
    }
}

@Composable
private fun TimePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VioletPillBg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text,
            color = VioletDeep,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/* ── lower list of recent dishes (на всякий — fallback / визуальный backstop) ── */

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
private fun DishRow(e: FoodEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(VioletPillBg),
            contentAlignment = Alignment.Center,
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
                Text("🍽", fontSize = 24.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                e.dish,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${timeFmt.format(Date(e.timestamp))}  ·  Б ${e.proteinG} · Ж ${e.fatG} · У ${e.carbsG}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        Text(
            "${e.kcal}",
            color = VioletPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
        )
        Text(
            " ккал",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.sp,
        )
    }
}
