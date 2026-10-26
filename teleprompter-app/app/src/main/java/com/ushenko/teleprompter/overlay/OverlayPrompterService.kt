package com.ushenko.teleprompter.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.IBinder
import android.view.Choreographer
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.ushenko.teleprompter.PrompterSettings
import com.ushenko.teleprompter.PrompterStore
import com.ushenko.teleprompter.R

/**
 * Draws the teleprompter in a window above other apps, so it can be used while recording
 * with the stock camera app (e.g. Pixel Camera on the front lens).
 */
class OverlayPrompterService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var store: PrompterStore
    private lateinit var settings: PrompterSettings
    private lateinit var params: WindowManager.LayoutParams
    private lateinit var scrollView: ScrollView
    private lateinit var scriptView: TextView
    private lateinit var playButton: TextView
    private lateinit var speedLabel: TextView
    private var root: View? = null

    private var playing = false
    private var lastFrameNanos = 0L
    private var pendingScroll = 0f

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!playing) return
            if (lastFrameNanos != 0L) {
                pendingScroll += dp(settings.speedDpPerSec) * (frameTimeNanos - lastFrameNanos) / 1_000_000_000f
                val step = pendingScroll.toInt()
                if (step > 0) {
                    scrollView.scrollBy(0, step)
                    pendingScroll -= step
                }
                if (!scrollView.canScrollVertically(1)) {
                    setPlaying(false)
                    return
                }
            }
            lastFrameNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        store = PrompterStore(this)
        settings = store.settings
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        startInForeground()
        if (root == null) {
            showOverlay()
        } else {
            // Re-launched from the app: pick up an edited script and start from the top.
            settings = store.settings
            scriptView.text = store.script
            scrollView.scrollTo(0, 0)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        setPlaying(false)
        root?.let { windowManager.removeView(it) }
        root = null
        super.onDestroy()
    }

    private fun startInForeground() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Плаваючий суфлер", NotificationManager.IMPORTANCE_LOW),
        )
        val stop = PendingIntent.getService(
            this,
            0,
            Intent(this, OverlayPrompterService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Суфлер поверх камери")
            .setContentText("Торкніться «Закрити», щоб прибрати текст з екрана")
            .setOngoing(true)
            .addAction(Notification.Action.Builder(null, "Закрити", stop).build())
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun showOverlay() {
        val screenHeight = resources.displayMetrics.heightPixels
        val textHeight = (screenHeight * settings.panelHeightFraction).toInt()

        scriptView = TextView(this).apply {
            text = store.script
            setTextColor(Color.WHITE)
            textSize = settings.fontSizeSp
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setLineSpacing(0f, 1.3f)
            // Top padding clears the camera hole; bottom padding lets the last line reach the top.
            setPadding(dp(24f).toInt(), dp(44f).toInt(), dp(24f).toInt(), textHeight)
            setOnClickListener { setPlaying(!playing) }
        }
        scrollView = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            addView(scriptView)
        }

        playButton = controlButton("▶") { setPlaying(!playing) }
        speedLabel = controlButton(speedText()) { }
        val dragHandle = controlButton("↕") { }
        dragHandle.setOnTouchListener(object : View.OnTouchListener {
            private var startY = 0
            private var touchStartY = 0f
            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        startY = params.y
                        touchStartY = event.rawY
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.y = (startY + (event.rawY - touchStartY)).toInt().coerceAtLeast(0)
                        root?.let { windowManager.updateViewLayout(it, params) }
                    }
                }
                return true
            }
        })

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.argb(200, 0, 0, 0))
            addView(dragHandle)
            addView(controlButton("⟲") {
                scrollView.scrollTo(0, 0)
                pendingScroll = 0f
            })
            addView(controlButton("−") { changeSpeed(-SPEED_STEP) })
            addView(playButton)
            addView(controlButton("+") { changeSpeed(SPEED_STEP) })
            addView(speedLabel)
            addView(controlButton("A−") { changeFont(-2f) })
            addView(controlButton("A+") { changeFont(2f) })
            addView(controlButton("✕") { stopSelf() })
        }

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb((settings.backgroundAlpha * 255).toInt(), 0, 0, 0))
            addView(scrollView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, textHeight))
            addView(controls, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            layoutInDisplayCutoutMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            } else {
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        windowManager.addView(panel, params)
        root = panel
    }

    private fun controlButton(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE)
        textSize = 18f
        gravity = Gravity.CENTER
        val pad = dp(10f).toInt()
        setPadding(pad, pad, pad, pad)
        setOnClickListener { onClick() }
    }

    private fun setPlaying(value: Boolean) {
        playing = value
        if (::playButton.isInitialized) playButton.text = if (value) "⏸" else "▶"
        val choreographer = Choreographer.getInstance()
        choreographer.removeFrameCallback(frameCallback)
        if (value) {
            lastFrameNanos = 0L
            choreographer.postFrameCallback(frameCallback)
        }
    }

    private fun changeSpeed(delta: Float) {
        settings = settings.copy(
            speedDpPerSec = (settings.speedDpPerSec + delta)
                .coerceIn(PrompterSettings.MIN_SPEED, PrompterSettings.MAX_SPEED),
        )
        speedLabel.text = speedText()
        store.settings = settings
    }

    private fun changeFont(delta: Float) {
        settings = settings.copy(
            fontSizeSp = (settings.fontSizeSp + delta).coerceIn(PrompterSettings.MIN_FONT, PrompterSettings.MAX_FONT),
        )
        scriptView.textSize = settings.fontSizeSp
        store.settings = settings
    }

    private fun speedText() = settings.speedDpPerSec.toInt().toString()

    private fun dp(value: Float) = value * resources.displayMetrics.density

    companion object {
        const val ACTION_STOP = "com.ushenko.teleprompter.action.STOP_OVERLAY"
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        private const val SPEED_STEP = 5f
    }
}
