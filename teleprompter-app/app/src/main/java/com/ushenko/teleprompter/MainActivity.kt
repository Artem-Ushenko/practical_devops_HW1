package com.ushenko.teleprompter

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.ushenko.teleprompter.overlay.OverlayPrompterService
import com.ushenko.teleprompter.ui.EditorScreen
import com.ushenko.teleprompter.ui.RecordScreen

class MainActivity : ComponentActivity() {

    private lateinit var store: PrompterStore

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { startOverlayAndCamera() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = PrompterStore(this)

        if (savedInstanceState == null && intent?.action == Intent.ACTION_SEND) {
            intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { store.script = it }
        }

        setContent {
            val colors = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(this)
            } else {
                darkColorScheme()
            }
            MaterialTheme(colorScheme = colors) {
                var script by remember { mutableStateOf(store.script) }
                var settings by remember { mutableStateOf(store.settings) }
                var recording by rememberSaveable { mutableStateOf(false) }

                // The floating overlay may have changed speed or font size while we were in the background.
                LifecycleResumeEffect(Unit) {
                    settings = store.settings
                    onPauseOrDispose { }
                }

                val updateSettings: (PrompterSettings) -> Unit = {
                    settings = it
                    store.settings = it
                }

                if (recording) {
                    RecordScreen(
                        script = script,
                        settings = settings,
                        onSettingsChange = updateSettings,
                        onBack = { recording = false },
                    )
                } else {
                    EditorScreen(
                        script = script,
                        onScriptChange = {
                            script = it
                            store.script = it
                        },
                        settings = settings,
                        onSettingsChange = updateSettings,
                        onRecord = { recording = true },
                        onFloatingPrompter = ::launchFloatingPrompter,
                    )
                }
            }
        }
    }

    /** Shows the prompter over other apps and opens the system (Pixel) camera in video mode. */
    private fun launchFloatingPrompter() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Дозвольте «Суфлеру» показ поверх інших додатків", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")),
            )
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            // The notification only carries a "close" action, so we continue whatever the answer is.
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startOverlayAndCamera()
    }

    private fun startOverlayAndCamera() {
        ContextCompat.startForegroundService(this, Intent(this, OverlayPrompterService::class.java))
        val camera = Intent(MediaStore.INTENT_ACTION_VIDEO_CAMERA)
            // Unofficial extras understood by Pixel Camera and several other camera apps.
            .putExtra("android.intent.extras.CAMERA_FACING", 1)
            .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
            .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        try {
            startActivity(camera)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Не знайдено додаток камери — відкрийте його вручну", Toast.LENGTH_LONG).show()
        }
    }
}
