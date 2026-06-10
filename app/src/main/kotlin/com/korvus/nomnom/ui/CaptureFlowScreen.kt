package com.korvus.nomnom.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
    data class Loading(val uri: Uri) : FlowState()
    data class Ready(val uri: Uri, val result: AnalysisResult) : FlowState()
    data class Error(val uri: Uri, val msg: String) : FlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = NomNomApp.instance

    val baseUrl by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val model by app.settings.model.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by app.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")

    var state by remember { mutableStateOf<FlowState>(FlowState.Idle) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun runAnalyze(uri: Uri) {
        state = FlowState.Loading(uri)
        app.appScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("не удалось прочитать фото")
                if (baseUrl.isBlank() || model.isBlank()) {
                    throw IllegalStateException("В настройках не задан Base URL или модель")
                }
                val result = VisionAnalyzer(baseUrl, model, apiKey).analyze(bytes)
                state = FlowState.Ready(uri, result)
            } catch (t: Throwable) {
                state = FlowState.Error(uri, t.message ?: t.toString())
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) runAnalyze(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok && cameraUri != null) runAnalyze(cameraUri!!) }

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
        when (val s = state) {
            FlowState.Idle -> IdleBody(
                paddingTop = padding,
                onCamera = ::startCamera,
                onGallery = {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
            is FlowState.Loading -> LoadingBody(padding, s.uri)
            is FlowState.Ready -> ResultBody(
                padding = padding,
                uri = s.uri,
                result = s.result,
                onSave = {
                    app.appScope.launch {
                        val img = saveImage(ctx, s.uri)
                        app.dayLog.add(
                            FoodEntry(
                                id = UUID.randomUUID().toString(),
                                timestamp = System.currentTimeMillis(),
                                dish = s.result.dish,
                                kcal = s.result.kcal,
                                proteinG = s.result.proteinG,
                                fatG = s.result.fatG,
                                carbsG = s.result.carbsG,
                                comment = s.result.comment,
                                confidence = s.result.confidence,
                                imagePath = img,
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "Сними тарелку",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            lineHeight = 30.sp,
        )
        Text(
            "Крупный план + хороший свет = точные ккал.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(8.dp))
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
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        // "Добавить в [Обед v]" — pill
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Добавить в",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
            Spacer(Modifier.weight(1f))
            MealPill(currentMeal())
        }
        Spacer(Modifier.height(14.dp))
        // Hero: фото слева 96dp + блок справа (ккал + 3 пилюли)
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
                    "${result.kcal} ккал",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    fontSize = 22.sp,
                )
                Spacer(Modifier.height(10.dp))
                MacroLine("Белки", "${result.proteinG} г", PinkRoseBg, PinkRoseText)
                Spacer(Modifier.height(6.dp))
                MacroLine("Жиры", "${result.fatG} г", PeachBg, PeachText)
                Spacer(Modifier.height(6.dp))
                MacroLine("Углеводы", "${result.carbsG} г", MintBg, MintText)
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Это: ${result.dish}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
        )
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
            onClick = onSave,
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
private fun MealPill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(VioletPale)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            "🍴  $text",
            color = VioletDeep,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private fun currentMeal(): String {
    val h = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (h) {
        in 5..10 -> "Завтрак"
        in 11..15 -> "Обед"
        in 16..21 -> "Ужин"
        else -> "Перекус"
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
