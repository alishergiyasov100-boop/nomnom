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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.api.ChefChat
import com.korvus.nomnom.data.startOfDayMillis
import com.korvus.nomnom.ui.theme.VioletDeep
import com.korvus.nomnom.ui.theme.VioletPale
import com.korvus.nomnom.ui.theme.VioletPrimary
import kotlinx.coroutines.launch

private data class ChatMsg(val role: String, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChefChatScreen() {
    val app = NomNomApp.instance
    val entries by app.dayLog.entries.collectAsState()
    val target by app.settings.dailyTarget.collectAsStateWithLifecycle(2000)
    val baseUrl by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val model by app.settings.model.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by app.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")

    val msgs = remember {
        mutableStateListOf(
            ChatMsg(
                "assistant",
                "Здарова. Я — Шеф. Вижу всё, что ты сегодня съел. Спрашивай — посчитаю, посоветую, или подколю.",
            )
        )
    }
    var input by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(msgs.size) {
        if (msgs.isNotEmpty()) listState.animateScrollToItem(msgs.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Шеф Пикстраль", fontWeight = FontWeight.Black, fontSize = 17.sp)
                        Text(
                            "видит твой дневник",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(msgs) { m -> MessageBubble(m) }
                if (sending) {
                    item { TypingIndicator() }
                }
            }
            InputBar(
                value = input,
                onChange = { input = it },
                enabled = !sending && baseUrl.isNotBlank() && model.isNotBlank(),
                onSend = {
                    val q = input.trim()
                    if (q.isEmpty()) return@InputBar
                    input = ""
                    msgs.add(ChatMsg("user", q))
                    sending = true
                    val startDay = startOfDayMillis()
                    val today = entries.filter { it.timestamp >= startDay }
                    val context = ChefChat.buildDiaryContext(today, target)
                    val history = msgs.map { it.role to it.text }
                    scope.launch {
                        try {
                            val reply = ChefChat(baseUrl, model, apiKey).ask(context, history)
                            msgs.add(ChatMsg("assistant", reply))
                        } catch (t: Throwable) {
                            msgs.add(ChatMsg("assistant", "⚠ Что-то пошло не так: ${t.message ?: t.toString()}"))
                        } finally {
                            sending = false
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMsg) {
    val isUser = m.role == "user"
    val align = if (isUser) Alignment.End else Alignment.Start
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = align) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(
                    topStart = 18.dp,
                    topEnd = 18.dp,
                    bottomStart = if (isUser) 18.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 18.dp,
                ))
                .background(if (isUser) VioletPrimary else VioletPale)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                m.text,
                color = if (isUser) Color.White else VioletDeep,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun TypingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(VioletPale)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = VioletPrimary,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Шеф думает…",
                    color = VioletDeep,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun InputBar(
    value: String,
    onChange: (String) -> Unit,
    enabled: Boolean,
    onSend: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.weight(1f),
            placeholder = { Text("Спроси у Шефа…", fontSize = 14.sp) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VioletPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            maxLines = 4,
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(50))
                .background(if (enabled && value.isNotBlank()) VioletPrimary else MaterialTheme.colorScheme.outline),
            contentAlignment = Alignment.Center,
        ) {
            IconButton(
                onClick = onSend,
                enabled = enabled && value.isNotBlank(),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "отправить",
                    tint = Color.White,
                )
            }
        }
    }
}
