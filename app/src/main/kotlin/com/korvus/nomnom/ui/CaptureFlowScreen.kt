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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
    data class Picked(val uri: Uri) : FlowState()
    data class Loading(val uri: Uri) : FlowState()
    data class Ready(val uri: Uri, val result: AnalysisResult) : FlowState()
    data class Error(val uri: Uri?, val msg: String) : FlowState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = NomNomApp.instance
    val scope = rememberCoroutineScope()

    val baseUrl by app.settings.baseUrl.collectAsStateWithLifecycle(initialValue = "")
    val model by app.settings.model.collectAsStateWithLifecycle(initialValue = "")
    val apiKey by app.settings.apiKey.collectAsStateWithLifecycle(initialValue = "")

    var state by remember { mutableStateOf<FlowState>(FlowState.Idle) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) state = FlowState.Picked(uri)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok && cameraUri != null) state = FlowState.Picked(cameraUri!!)
    }

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
            == PackageManager.PERMISSION_GRANTED) {
            val uri = createCameraOutputUri(ctx)
            cameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            permLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(state) {
        val s = state
        if (s is FlowState.Picked && baseUrl.isNotBlank() && model.isNotBlank()) {
            state = FlowState.Loading(s.uri)
            try {
                val bytes = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(s.uri)?.use { it.readBytes() }
                } ?: throw IllegalStateException("не удалось прочитать фото")
                val analyzer = VisionAnalyzer(baseUrl, model, apiKey)
                val result = analyzer.analyze(bytes)
                state = FlowState.Ready(s.uri, result)
            } catch (t: Throwable) {
                state = FlowState.Error(s.uri, t.message ?: t.toString())
            }
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val s = state) {
                FlowState.Idle -> PickerStub(onCamera = ::startCamera) {
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is FlowState.Picked -> PreviewBlock(s.uri, statusLine = "Готовлю к анализу…")
                is FlowState.Loading -> PreviewBlock(s.uri, statusLine = "Мика-нейронка нюхает фото…", loading = true)
                is FlowState.Ready -> ResultBlock(
                    uri = s.uri, result = s.result,
                    onSave = {
                        scope.launch {
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
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Сфоткай или выбери из галереи",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Лучше — крупный план, чтобы было видно состав. Нейронка любит фактуру 🍝",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onCamera,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(Icons.Default.PhotoCamera, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Камера")
        }
        OutlinedButton(onClick = onGallery, modifier = Modifier.weight(1f)) {
            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Галерея")
        }
    }
}

@Composable
private fun PreviewBlock(uri: Uri, statusLine: String, loading: Boolean = false) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.width(20.dp).height(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            statusLine,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun ResultBlock(
    uri: Uri,
    result: AnalysisResult,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                result.dish,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 22.sp,
            )
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    result.kcal.toString(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 48.sp,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "ккал",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Row {
                Macro("Б", result.proteinG)
                Spacer(Modifier.width(14.dp))
                Macro("Ж", result.fatG)
                Spacer(Modifier.width(14.dp))
                Macro("У", result.carbsG)
            }
        }
    }

    if (result.comment.isNotBlank()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Комментарий",
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    result.comment,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onRetry, modifier = Modifier.weight(1f)) { Text("Ещё раз") }
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        ) { Text("В дневник", fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun Macro(label: String, value: Int) {
    Row(verticalAlignment = Alignment.Bottom) {
        Text(
            "$label ",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            value.toString(),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
        )
        Text(
            "г",
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 2.dp, start = 1.dp),
        )
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
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                "Не получилось распознать",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(Modifier.height(4.dp))
            Text(msg, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        }
    }
    TextButton(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Попробовать ещё") }
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
