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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        // Application scope — переживает уход с экрана / смену state в Composable
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
                title = { Text("Новое блюдо", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            when (val s = state) {
                FlowState.Idle -> PickerStub(onCamera = ::startCamera) {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is FlowState.Loading -> PreviewBlock(s.uri)
                is FlowState.Ready -> ResultBlock(
                    uri = s.uri, result = s.result,
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
                is FlowState.Error -> ErrorBlock(s.uri, s.msg) { state = FlowState.Idle }
            }
        }
    }
}

@Composable
private fun PickerStub(onCamera: () -> Unit, onGallery: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                "Сфоткай или выбери из галереи",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 17.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Чем крупнее план — тем точнее ккал. Свет / угол важны 🍝",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onCamera,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Камера", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onGallery,
            modifier = Modifier.weight(1f).height(56.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Галерея", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PreviewBlock(uri: Uri) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.BottomStart,
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        "Нюхаю фото…",
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                    Text(
                        "Qwen Vision считает калории",
                        color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultBlock(
    uri: Uri,
    result: AnalysisResult,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 11f)
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }

    // dish + ккал — главный hero
    Column {
        Text(
            result.dish,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            lineHeight = 32.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                result.kcal.toString(),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Black,
                fontSize = 64.sp,
                lineHeight = 64.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "ккал",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }
    }

    // макро-ряд: три пилюли
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroChip("белки", result.proteinG, Modifier.weight(1f))
        MacroChip("жиры", result.fatG, Modifier.weight(1f))
        MacroChip("углеводы", result.carbsG, Modifier.weight(1f))
    }

    // комментарий — без рамки, как заметка от шефа
    if (result.comment.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "от шефа",
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result.comment,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.weight(1f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
        ) { Text("Ещё раз") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1.4f).height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) { Text("В дневник", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun MacroChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
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
private fun ErrorBlock(uri: Uri?, msg: String, onRetry: () -> Unit) {
    if (uri != null) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Не получилось распознать",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                msg,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
            )
        }
    }
    TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
        Text("Попробовать снова")
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
