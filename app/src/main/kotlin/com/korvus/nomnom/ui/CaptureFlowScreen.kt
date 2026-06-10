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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.korvus.nomnom.NomNomApp
import com.korvus.nomnom.api.VisionAnalyzer
import com.korvus.nomnom.data.AnalysisResult
import com.korvus.nomnom.data.FoodEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

private sealed class FlowState {
    object Idle : FlowState()
    data class Loading(val uri: Uri) : FlowState()
    data class Ready(val uri: Uri, val result: AnalysisResult) : FlowState()
    data class Error(val uri: Uri?, val msg: String) : FlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowScreen(nav: NavController) {
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

    when (val s = state) {
        FlowState.Idle -> IdleScreen(nav, onCamera = ::startCamera) {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        is FlowState.Loading -> ImageWithSheet(
            uri = s.uri,
            onBack = { nav.popBackStack() },
            sheet = { LoadingSheet() }
        )
        is FlowState.Ready -> ImageWithSheet(
            uri = s.uri,
            onBack = { nav.popBackStack() },
            sheet = {
                ResultSheet(
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
                            nav.popBackStack()
                        }
                    },
                    onRetry = { state = FlowState.Idle }
                )
            }
        )
        is FlowState.Error -> ImageWithSheet(
            uri = s.uri,
            onBack = { nav.popBackStack() },
            sheet = { ErrorSheet(s.msg) { state = FlowState.Idle } }
        )
    }
}

/* ── IDLE: чистый экран с двумя крупными action-картами ── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IdleScreen(
    nav: NavController,
    onCamera: () -> Unit,
    onGallery: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новое блюдо", fontWeight = FontWeight.SemiBold, fontSize = 18.sp) },
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Сними тарелку",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                "Чем крупнее план и ярче свет — тем точнее ккал.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(8.dp))
            ActionTile(
                icon = Icons.Default.PhotoCamera,
                title = "Камера",
                subtitle = "Снять прямо сейчас",
                primary = true,
                onClick = onCamera,
            )
            ActionTile(
                icon = Icons.Default.PhotoLibrary,
                title = "Галерея",
                subtitle = "Выбрать готовое фото",
                primary = false,
                onClick = onGallery,
            )
        }
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    primary: Boolean,
    onClick: () -> Unit,
) {
    val container = if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val content = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onBackground
    val subtle = if (primary) content.copy(alpha = 0.75f) else MaterialTheme.colorScheme.onSurfaceVariant
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(content.copy(alpha = if (primary) 0.18f else 0.06f)),
                contentAlignment = Alignment.Center,
            ) { Icon(icon, contentDescription = null, tint = content) }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = content, fontSize = 17.sp)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, color = subtle, fontSize = 13.sp)
            }
        }
    }
}

/* ── Universal screen: large hero photo + bottom sheet ── */
@Composable
private fun ImageWithSheet(
    uri: Uri,
    onBack: () -> Unit,
    sheet: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // hero фото — edge-to-edge, верхняя половина экрана
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            // лёгкий градиент-фейд от низа к фото (читаемость)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.7f to Color.Transparent,
                            1f to MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                        )
                    )
            )
            // back-кнопка поверх фото в стеклянной таблетке
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f)),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "назад",
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }

        // bottom sheet, выезжает поверх фото на 24dp
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 0.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .width(36.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.height(14.dp))
                    sheet()
                }
            }
        }
    }
}

@Composable
private fun LoadingSheet() {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "Анализирую блюдо…",
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                )
                Text(
                    "Qwen Vision считает калории и БЖУ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun ResultSheet(
    result: AnalysisResult,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            result.dish,
            fontWeight = FontWeight.Black,
            fontSize = 26.sp,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 30.sp,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                result.kcal.toString(),
                fontWeight = FontWeight.Black,
                fontSize = 56.sp,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 58.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "ккал",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroChip("белки", result.proteinG, Modifier.weight(1f))
            MacroChip("жиры", result.fatG, Modifier.weight(1f))
            MacroChip("углеводы", result.carbsG, Modifier.weight(1f))
        }
        if (result.comment.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "ОТ ШЕФА",
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        result.comment,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRetry,
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
            ) { Text("Ещё раз", fontWeight = FontWeight.SemiBold) }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1.4f).height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            ) { Text("В дневник", fontWeight = FontWeight.SemiBold, fontSize = 15.sp) }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun MacroChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp, horizontal = 12.dp),
    ) {
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
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp,
            )
            Text(
                " г",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
    }
}

@Composable
private fun ErrorSheet(msg: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Не получилось распознать",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
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
            shape = RoundedCornerShape(16.dp),
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
