package com.ushenko.teleprompter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ushenko.teleprompter.PrompterSettings
import kotlin.math.roundToInt

@Composable
fun SettingsPanel(
    settings: PrompterSettings,
    onChange: (PrompterSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text("Налаштування суфлера", style = MaterialTheme.typography.titleLarge)
        LabeledSlider(
            label = "Швидкість прокрутки",
            valueText = "${settings.speedDpPerSec.roundToInt()}",
            value = settings.speedDpPerSec,
            range = PrompterSettings.MIN_SPEED..PrompterSettings.MAX_SPEED,
        ) { onChange(settings.copy(speedDpPerSec = it)) }
        LabeledSlider(
            label = "Розмір шрифту",
            valueText = "${settings.fontSizeSp.roundToInt()} sp",
            value = settings.fontSizeSp,
            range = PrompterSettings.MIN_FONT..PrompterSettings.MAX_FONT,
        ) { onChange(settings.copy(fontSizeSp = it)) }
        LabeledSlider(
            label = "Висота панелі з текстом",
            valueText = "${(settings.panelHeightFraction * 100).roundToInt()}%",
            value = settings.panelHeightFraction,
            range = 0.2f..0.7f,
        ) { onChange(settings.copy(panelHeightFraction = it)) }
        LabeledSlider(
            label = "Затемнення фону під текстом",
            valueText = "${(settings.backgroundAlpha * 100).roundToInt()}%",
            value = settings.backgroundAlpha,
            range = 0f..0.9f,
        ) { onChange(settings.copy(backgroundAlpha = it)) }
        LabeledSlider(
            label = "Зворотний відлік перед записом",
            valueText = "${settings.countdownSeconds} с",
            value = settings.countdownSeconds.toFloat(),
            range = 0f..10f,
            steps = 9,
        ) { onChange(settings.copy(countdownSeconds = it.roundToInt())) }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit,
) {
    Column(Modifier.padding(top = 12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = range, steps = steps)
    }
}
