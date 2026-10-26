package com.ushenko.teleprompter.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.PackageManager
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ushenko.teleprompter.PrompterSettings
import com.ushenko.teleprompter.ScriptStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "RecordScreen"
private const val SPEED_STEP = 5f

@Composable
fun RecordScreen(
    script: String,
    settings: PrompterSettings,
    onSettingsChange: (PrompterSettings) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasCamera by remember { mutableStateOf(context.isGranted(Manifest.permission.CAMERA)) }
    var hasAudio by remember { mutableStateOf(context.isGranted(Manifest.permission.RECORD_AUDIO)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasCamera = result[Manifest.permission.CAMERA] ?: hasCamera
        hasAudio = result[Manifest.permission.RECORD_AUDIO] ?: hasAudio
    }
    LaunchedEffect(Unit) {
        if (!hasCamera || !hasAudio) {
            permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
        }
    }

    ImmersiveKeepScreenOn()

    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val videoCapture = remember {
        val recorder = Recorder.Builder()
            .setQualitySelector(
                QualitySelector.from(Quality.FHD, FallbackStrategy.lowerQualityOrHigherThan(Quality.HD)),
            )
            .build()
        VideoCapture.withOutput(recorder)
    }

    var recording by remember { mutableStateOf<Recording?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    var countdown by remember { mutableStateOf<Int?>(null) }
    var playing by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    DisposableEffect(hasCamera, lifecycleOwner) {
        if (!hasCamera) return@DisposableEffect onDispose { }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, videoCapture)
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(context, "Не вдалося відкрити фронтальну камеру", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(context))
        onDispose {
            recording?.stop()
            if (providerFuture.isDone) providerFuture.get().unbindAll()
        }
    }

    @SuppressLint("MissingPermission") // RECORD_AUDIO is checked via hasAudio.
    fun startRecording() {
        val name = "Prompter_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Teleprompter")
        }
        val output = MediaStoreOutputOptions
            .Builder(context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(values)
            .build()
        var pending = videoCapture.output.prepareRecording(context, output)
        if (hasAudio) pending = pending.withAudioEnabled()
        elapsedSeconds = 0
        recording = pending.start(ContextCompat.getMainExecutor(context)) { event ->
            when (event) {
                is VideoRecordEvent.Start -> isRecording = true
                is VideoRecordEvent.Status ->
                    elapsedSeconds = (event.recordingStats.recordedDurationNanos / 1_000_000_000L).toInt()
                is VideoRecordEvent.Finalize -> {
                    isRecording = false
                    recording = null
                    val message = if (event.hasError()) {
                        Log.e(TAG, "Recording failed: ${event.error}", event.cause)
                        "Помилка запису (код ${event.error})"
                    } else {
                        "Відео збережено в Movies/Teleprompter"
                    }
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
        playing = true
    }

    fun stopRecording() {
        recording?.stop()
        playing = false
    }

    LaunchedEffect(countdown) {
        val value = countdown ?: return@LaunchedEffect
        if (value > 0) {
            delay(1000)
            countdown = value - 1
        } else {
            countdown = null
            startRecording()
        }
    }

    BackHandler {
        if (recording != null) stopRecording()
        onBack()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (hasCamera) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Потрібен доступ до камери та мікрофона", color = Color.White, fontSize = 18.sp)
                Button(
                    onClick = {
                        permissionLauncher.launch(
                            arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                        )
                    },
                    modifier = Modifier.padding(top = 16.dp),
                ) { Text("Надати доступ") }
            }
        }

        BoxWithConstraints(Modifier.fillMaxSize()) {
            // The panel hugs the top edge so the text is right next to the front camera lens.
            PrompterText(
                text = script,
                fontSizeSp = settings.fontSizeSp,
                speedDpPerSec = settings.speedDpPerSec,
                playing = playing,
                scrollState = scrollState,
                readingLineOffset = 12.dp,
                onTap = { playing = !playing },
                onReachedEnd = { playing = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeight * settings.panelHeightFraction)
                    .background(Color.Black.copy(alpha = settings.backgroundAlpha))
                    .windowInsetsPadding(WindowInsets.displayCutout),
            )
        }

        countdown?.let {
            Text(
                text = it.toString(),
                color = Color.White,
                fontSize = 120.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.35f))
                .navigationBarsPadding()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isRecording) {
                Text(
                    "● " + ScriptStats.formatDuration(elapsedSeconds),
                    color = Color(0xFFFF5252),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = {
                    if (recording != null) stopRecording()
                    onBack()
                }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Назад", tint = Color.White)
                }
                IconButton(onClick = { scope.launch { scrollState.animateScrollTo(0) } }) {
                    Icon(Icons.Filled.Replay, "На початок", tint = Color.White)
                }
                RecordButton(
                    recording = isRecording || recording != null,
                    countingDown = countdown != null,
                    enabled = hasCamera,
                    onClick = {
                        when {
                            recording != null -> stopRecording()
                            countdown != null -> countdown = null
                            settings.countdownSeconds > 0 -> countdown = settings.countdownSeconds
                            else -> startRecording()
                        }
                    },
                )
                IconButton(onClick = { playing = !playing }) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playing) "Пауза прокрутки" else "Старт прокрутки",
                        tint = Color.White,
                    )
                }
                Text(
                    "A±",
                    color = Color.White,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            val next = settings.fontSizeSp + 4f
                            onSettingsChange(
                                settings.copy(
                                    fontSizeSp = if (next > PrompterSettings.MAX_FONT) PrompterSettings.MIN_FONT else next,
                                ),
                            )
                        }
                        .padding(12.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    onSettingsChange(
                        settings.copy(
                            speedDpPerSec = (settings.speedDpPerSec - SPEED_STEP)
                                .coerceAtLeast(PrompterSettings.MIN_SPEED),
                        ),
                    )
                }) { Icon(Icons.Filled.Remove, "Повільніше", tint = Color.White) }
                Text(
                    "Швидкість ${settings.speedDpPerSec.toInt()}",
                    color = Color.White,
                    fontSize = 16.sp,
                )
                IconButton(onClick = {
                    onSettingsChange(
                        settings.copy(
                            speedDpPerSec = (settings.speedDpPerSec + SPEED_STEP)
                                .coerceAtMost(PrompterSettings.MAX_SPEED),
                        ),
                    )
                }) { Icon(Icons.Filled.Add, "Швидше", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun RecordButton(recording: Boolean, countingDown: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(72.dp)
            .border(4.dp, Color.White, CircleShape)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val inner = if (recording) {
            Modifier.size(28.dp).background(Color(0xFFFF5252), RoundedCornerShape(6.dp))
        } else {
            Modifier.size(56.dp).background(
                if (countingDown) Color(0xFFFF8A80) else Color(0xFFFF5252),
                CircleShape,
            )
        }
        Box(inner)
    }
}

/** Hides system bars and keeps the display awake while the recording screen is shown. */
@Composable
private fun ImmersiveKeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }
}

private fun Context.isGranted(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
