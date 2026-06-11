package com.korvus.nomnom.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.api.VisionAnalyzer
import com.korvus.nomnom.data.AnalysisResult
import com.korvus.nomnom.data.FoodEntry
import com.korvus.nomnom.ui.theme.MintBg
import com.korvus.nomnom.ui.theme.MintText
import com.korvus.nomnom.ui.theme.PeachBg
import com.korvus.nomnom.ui.theme.PeachText
import com.korvus.nomnom.ui.theme.PinkRoseBg
import com.korvus.nomnom.ui.theme.PinkRoseText
import com.korvus.nomnom.ui.theme.VioletDeep
import com.korvus.nomnom.ui.theme.VioletPale
import com.korvus.nomnom.ui.theme.VioletPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private sealed class FlowState {
    object Idle : FlowState()
    data class Drafting(val uris: List<Uri>) : FlowState()
    data class Loading(val uris: List<Uri>) : FlowState()
    data class Ready(val uris: List<Uri>, val result: AnalysisResult) : FlowState()
    data class Error(val uris: List<Uri>, val msg: String) : FlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = NomNomApp.instance

    val baseUrl by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val model by app.settings.model.collectAsStateWithLifecycle(initialValue = "")
    val apiKeys by app.settings.apiKeys.collectAsStateWithLifecycle(initialValue = emptyList())

    var state by remember { mutableStateOf<FlowState>(FlowState.Idle) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun addUri(uri: Uri) {
        state = when (val s = state) {
            is FlowState.Drafting -> FlowState.Drafting(s.uris + uri)
            else -> FlowState.Drafting(listOf(uri))
        }
    }

    fun runAnalyze(uris: List<Uri>) {
        state = FlowState.Loading(uris)
        app.appScope.launch {
            try {
                val imgs = withContext(Dispatchers.IO) {
                    uris.mapNotNull { u ->
                        ctx.contentResolver.openInputStream(u)?.use { it.readBytes() }
                    }
                }
                if (imgs.isEmpty()) throw IllegalStateException("не удалось прочитать фото")
                if (baseUrl.isBlank() || model.isBlank()) {
                    throw IllegalStateException("В настройках не задан Base URL или модель")
                }
                val startIdx = app.settings.currentRotationIdx()
                val result = VisionAnalyzer(
                    baseUrl, model, apiKeys, startIdx,
                    onAdvance = { app.settings.advanceRotation() },
                ).analyze(imgs)
                state = FlowState.Ready(uris, result)
            } catch (t: Throwable) {
                state = FlowState.Error(uris, t.message ?: t.toString())
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) addUri(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok && cameraUri != null) addUri(cameraUri!!) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val uri = createCameraOutputUri(ctx)
            cameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun startCamera() {
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val uri = createCameraOutputUri(ctx)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (state) {
                            FlowState.Idle -> "Новое блюдо"
                            is FlowState.Drafting -> "Ракурсы (${(state as FlowState.Drafting).uris.size})"
                            is FlowState.Loading -> "Сканирую…"
                            is FlowState.Ready -> "Результаты"
                            is FlowState.Error -> "Ошибка"
                        },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
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
                actions = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.Close, contentDescription = "закрыть")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        AnimatedContent(
            targetState = state,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 10 })
                    .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(220)) { -it / 12 })
            },
            label = "capture-flow",
        ) { s ->
            when (s) {
            FlowState.Idle -> IdleBody(
                paddingTop = padding,
                onCamera = ::startCamera,
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            is FlowState.Drafting -> DraftingBody(
                padding = padding,
                uris = s.uris,
                onAddCamera = ::startCamera,
                onAddGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onRemove = { idx ->
                    val left = s.uris.toMutableList().also { it.removeAt(idx) }
                    state = if (left.isEmpty()) FlowState.Idle else FlowState.Drafting(left)
                },
                onAnalyze = { runAnalyze(s.uris) },
            )
            is FlowState.Loading -> LoadingBody(padding, s.uris.first())
            is FlowState.Ready -> ResultBody(
                padding = padding,
                uri = s.uris.first(),
                result = s.result,
                onSave = { meal, edited ->
                    app.appScope.launch {
                        val img = saveImage(ctx, s.uris.first())
                        val totalK = edited.sumOf { it.kcal }
                        val totalP = edited.sumOf { it.proteinG }
                        val totalF = edited.sumOf { it.fatG }
                        val totalC = edited.sumOf { it.carbsG }
                        app.dayLog.add(
                            FoodEntry(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                dish = s.result.dish,
                                kcal = if (totalK > 0) totalK else s.result.kcal,
                                proteinG = if (totalP > 0) totalP else s.result.proteinG,
                                fatG = if (totalF > 0) totalF else s.result.fatG,
                                carbsG = if (totalC > 0) totalC else s.result.carbsG,
                                comment = s.result.comment,
                                confidence = s.result.confidence,
                                imagePath = img,
                                meal = meal,
                            )
                        )
                        onBack()
                    }
                },
                onRetry = { state = FlowState.Idle }
            )
            is FlowState.Error -> ErrorBody(padding, s.msg) { state = FlowState.Idle }
            }
        }
    }
}

@Composable
private fun IdleBody(
    paddingTop: androidx.compose.foundation.layout.PaddingValues,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingTop)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // illustration
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(VioletPale),
            contentAlignment = Alignment.Center,
        ) {
            Text("🍽️", fontSize = 84.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Сними тарелку",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        )
        Text(
            "Крупный план + хороший свет = точные ккал.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        )
        TipCard(
            "🥄  Положи ложку или монету рядом — модель оценит размер порции в 2× точнее.",
        )
        TipCard(
            "📐  После первого фото можно добавить ещё 1-2 ракурса (сбоку) — поможет увидеть объём.",
        )
        Spacer(Modifier.height(4.dp))
        ActionTile(
            icon = Icons.Outlined.PhotoCamera,
            title = "Сделать фото",
            subtitle = "Открыть камеру",
            primary = true,
            onClick = onCamera,
        )
        ActionTile(
            icon = Icons.Outlined.PhotoLibrary,
            title = "Из галереи",
            subtitle = "Выбрать готовое",
            primary = false,
            onClick = onGallery,
        )
    }
}

@Composable
private fun TipCard(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(VioletPale)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Text(
            text,
            color = VioletDeep,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val onBg = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    val subtle = if (primary) onBg.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(onBg.copy(alpha = if (primary) 0.18f else 0.06f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = onBg) }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = onBg, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = subtle, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DraftingBody(
    padding: androidx.compose.foundation.layout.PaddingValues,
    uris: List<Uri>,
    onAddCamera: () -> Unit,
    onAddGallery: () -> Unit,
    onRemove: (Int) -> Unit,
    onAnalyze: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Добавь ракурсы",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 22.sp,
        )
        Text(
            "До 3 фото = модель видит объём. Сейчас: ${uris.size}/3.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(uris) { idx, u ->
                Box {
                    AsyncImage(
                        model = u,
                        contentDescription = null,
                        modifier = Modifier
                            .size(140.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop,
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.55f))
                            .clickable { onRemove(idx) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "удалить", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        if (uris.size < 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onAddCamera,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = VioletPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Камера", color = VioletPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onAddGallery,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null, tint = VioletPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text("Галерея", color = VioletPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onAnalyze,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = Color.White,
            ),
        ) {
            Text(
                "Анализировать (${uris.size} ${plural(uris.size, "фото", "фото", "фото")})",
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

private fun plural(n: Int, one: String, few: String, many: String): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> one
        mod10 in 2..4 && mod100 !in 12..14 -> few
        else -> many
    }
}

@Composable
private fun LoadingBody(padding: androidx.compose.foundation.layout.PaddingValues, uri: Uri) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentScale = ContentScale.Crop,
        )
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Анализирую блюдо…", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    "Qwen Vision считает калории",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun ResultBody(
    padding: androidx.compose.foundation.layout.PaddingValues,
    uri: Uri,
    result: AnalysisResult,
    onSave: (String, List<com.korvus.nomnom.data.Component>) -> Unit,
    onRetry: () -> Unit,
) {
    var selectedMeal by remember { mutableStateOf(currentMeal()) }
    val components = remember(result) {
        mutableStateListOf<com.korvus.nomnom.data.Component>().also { it.addAll(result.components) }
    }
    val originals = remember(result) { result.components }
    val totalK = components.sumOf { it.kcal }
    val totalP = components.sumOf { it.proteinG }
    val totalF = components.sumOf { it.fatG }
    val totalC = components.sumOf { it.carbsG }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Добавить в",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.weight(1f))
            MealDropdown(selected = selectedMeal, onSelect = { selectedMeal = it })
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier
                    .size(106.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop,
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "$totalK ккал",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(10.dp))
                MacroLine("Белки", "$totalP г", PinkRoseBg, PinkRoseText)
                Spacer(Modifier.height(6.dp))
                MacroLine("Жиры", "$totalF г", PeachBg, PeachText)
                Spacer(Modifier.height(6.dp))
                MacroLine("Углеводы", "$totalC г", MintBg, MintText)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Это: ${result.dish}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
        if (components.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "СОСТАВ — крути граммы под себя",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(8.dp))
            components.forEachIndexed { idx, c ->
                ComponentRow(
                    comp = c,
                    original = originals.getOrNull(idx),
                    onAdjust = { newGrams ->
                        val baseG = originals.getOrNull(idx)?.grams ?: c.grams
                        val baseK = originals.getOrNull(idx)?.kcal ?: c.kcal
                        val baseP = originals.getOrNull(idx)?.proteinG ?: c.proteinG
                        val baseF = originals.getOrNull(idx)?.fatG ?: c.fatG
                        val baseC = originals.getOrNull(idx)?.carbsG ?: c.carbsG
                        if (baseG > 0) {
                            val r = newGrams.toDouble() / baseG.toDouble()
                            components[idx] = c.copy(
                                grams = newGrams,
                                kcal = (baseK * r).toInt(),
                                proteinG = (baseP * r).toInt(),
                                fatG = (baseF * r).toInt(),
                                carbsG = (baseC * r).toInt(),
                            )
                        } else {
                            components[idx] = c.copy(grams = newGrams)
                        }
                    },
                    onRemove = { components.removeAt(idx) },
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        if (result.comment.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = VioletPale),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ОТ ШЕФА",
                        color = VioletDeep.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.4.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        result.comment,
                        color = VioletDeep,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onSave(selectedMeal, components.toList()) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = VioletPrimary,
                contentColor = Color.White,
            ),
        ) { Text("Сохранить", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(50),
        ) { Text("Сканировать заново", color = VioletPrimary, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun ComponentRow(
    comp: com.korvus.nomnom.data.Component,
    original: com.korvus.nomnom.data.Component?,
    onAdjust: (Int) -> Unit,
    onRemove: () -> Unit,
) {
    val step = when {
        comp.grams < 30 -> 5
        comp.grams < 100 -> 10
        else -> 25
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                comp.name,
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            val changed = original != null && original.grams != comp.grams
            Text(
                if (changed && original != null) "${comp.kcal} ккал · ${comp.grams}г (было ${original.grams}г)"
                else "${comp.kcal} ккал · ${comp.grams}г",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
            )
        }
        AdjustButton("−") {
            val v = (comp.grams - step).coerceAtLeast(0)
            if (v == 0) onRemove() else onAdjust(v)
        }
        Spacer(Modifier.width(6.dp))
        AdjustButton("+") { onAdjust(comp.grams + step) }
    }
}

@Composable
private fun AdjustButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(50))
            .background(VioletPale)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = VioletDeep, fontWeight = FontWeight.Black, fontSize = 16.sp)
    }
}

@Composable
private fun MacroLine(label: String, value: String, bg: Color, fg: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text(value, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MealDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("Завтрак", "Обед", "Ужин", "Перекус")
    Box {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(VioletPale)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Text(
                "🍴  $selected  ▾",
                color = VioletDeep,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

private fun currentMeal(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..10 -> "Завтрак"
        in 11..15 -> "Обед"
        else -> "Ужин"  // 16-23 + 0-4 (поздний ужин по умолчанию)
    }
}

@Composable
private fun ErrorBody(
    padding: androidx.compose.foundation.layout.PaddingValues,
    msg: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            "Не получилось распознать",
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            msg,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            fontSize = 13.sp,
            lineHeight = 18.sp,
        )
        Spacer(Modifier.height(18.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text("Попробовать снова", fontWeight = FontWeight.SemiBold) }
    }
}

private fun createCameraOutputUri(ctx: Context): Uri {
    val dir = File(ctx.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "shot_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
}

private suspend fun saveImage(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    try {
        val dir = File(ctx.filesDir, "shots").apply { mkdirs() }
        val out = File(dir, "img_${System.currentTimeMillis()}.jpg")
        ctx.contentResolver.openInputStream(uri)?.use { input ->
            out.outputStream().use { input.copyTo(it) }
        }
        out.absolutePath
    } catch (_: Throwable) { null }
}
