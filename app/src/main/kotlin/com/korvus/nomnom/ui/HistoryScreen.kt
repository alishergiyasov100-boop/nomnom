package com.korvus.nomnom.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.data.FoodEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(nav: NavController) {
    val app = NomNomApp.instance
    val entries by app.dayLog.entries.collectAsState()
    val grouped = entries.groupBy { dayKey(it.timestamp) }.toSortedMap(compareByDescending { it })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("История", fontWeight = FontWeight.SemiBold) },
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
        if (entries.isEmpty()) {
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
                Text(
                    "Пусто — ещё ничего не съел 🍳",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Medium,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Сфоткай блюдо на главном экране — оно появится здесь.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            grouped.forEach { (_, day) ->
                val first = day.first()
                val totalKcal = day.sumOf { it.kcal }
                item { DayHeader(first.timestamp, totalKcal, day.size) }
                day.forEach { e -> item { EntryRow(e) } }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}

private val dayHeaderFmt = SimpleDateFormat("d MMMM, EEEE", Locale("ru"))

@Composable
private fun DayHeader(timestamp: Long, totalKcal: Int, count: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                dayHeaderFmt.format(Date(timestamp)).replaceFirstChar { it.titlecase(Locale("ru")) },
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "$totalKcal ккал · $count записей",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
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
