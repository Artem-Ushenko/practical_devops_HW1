package com.ushenko.teleprompter

import android.content.Context

/** User-tunable teleprompter options, shared by the in-app recorder and the floating overlay. */
data class PrompterSettings(
    /** Scroll speed in dp per second. */
    val speedDpPerSec: Float = 40f,
    val fontSizeSp: Float = 30f,
    /** Height of the prompter panel as a fraction of the screen height. */
    val panelHeightFraction: Float = 0.38f,
    /** Opacity of the dark background behind the text (0..1). */
    val backgroundAlpha: Float = 0.55f,
    val countdownSeconds: Int = 3,
) {
    companion object {
        const val MIN_SPEED = 5f
        const val MAX_SPEED = 200f
        const val MIN_FONT = 16f
        const val MAX_FONT = 64f
    }
}

/** Persists the script text and [PrompterSettings] in SharedPreferences. */
class PrompterStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("prompter", Context.MODE_PRIVATE)

    var script: String
        get() = prefs.getString(KEY_SCRIPT, null) ?: DEFAULT_SCRIPT
        set(value) = prefs.edit().putString(KEY_SCRIPT, value).apply()

    var settings: PrompterSettings
        get() {
            val d = PrompterSettings()
            return PrompterSettings(
                speedDpPerSec = prefs.getFloat(KEY_SPEED, d.speedDpPerSec),
                fontSizeSp = prefs.getFloat(KEY_FONT, d.fontSizeSp),
                panelHeightFraction = prefs.getFloat(KEY_HEIGHT, d.panelHeightFraction),
                backgroundAlpha = prefs.getFloat(KEY_ALPHA, d.backgroundAlpha),
                countdownSeconds = prefs.getInt(KEY_COUNTDOWN, d.countdownSeconds),
            )
        }
        set(value) = prefs.edit()
            .putFloat(KEY_SPEED, value.speedDpPerSec)
            .putFloat(KEY_FONT, value.fontSizeSp)
            .putFloat(KEY_HEIGHT, value.panelHeightFraction)
            .putFloat(KEY_ALPHA, value.backgroundAlpha)
            .putInt(KEY_COUNTDOWN, value.countdownSeconds)
            .apply()

    private companion object {
        const val KEY_SCRIPT = "script"
        const val KEY_SPEED = "speed"
        const val KEY_FONT = "font"
        const val KEY_HEIGHT = "height"
        const val KEY_ALPHA = "alpha"
        const val KEY_COUNTDOWN = "countdown"

        const val DEFAULT_SCRIPT = "Привіт! Це суфлер для запису відео на фронтальну камеру.\n\n" +
            "Вставте сюди свій текст, натисніть «Записати відео» — і текст почне " +
            "плавно прокручуватися вгорі екрана, поруч із камерою. Так погляд " +
            "залишається майже в об'єктив.\n\n" +
            "Торкніться тексту, щоб поставити прокрутку на паузу. Проведіть пальцем, " +
            "щоб перемотати вручну. Кнопками «−» і «+» змінюйте швидкість просто під час запису."
    }
}
