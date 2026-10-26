package com.ushenko.teleprompter.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Auto-scrolling script text. The line currently being read sits at [readingLineOffset]
 * from the top of the panel, i.e. right under the front camera.
 */
@Composable
fun PrompterText(
    text: String,
    fontSizeSp: Float,
    speedDpPerSec: Float,
    playing: Boolean,
    scrollState: ScrollState,
    readingLineOffset: Dp,
    onTap: () -> Unit,
    onReachedEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pxPerSec = with(LocalDensity.current) { speedDpPerSec.dp.toPx() }
    val currentOnReachedEnd by rememberUpdatedState(onReachedEnd)

    LaunchedEffect(playing, pxPerSec) {
        if (!playing) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            scrollState.dispatchRawDelta(pxPerSec * (now - last) / 1_000_000_000f)
            last = now
            if (scrollState.maxValue in 1..scrollState.value) {
                currentOnReachedEnd()
                break
            }
        }
    }

    BoxWithConstraints(modifier) {
        val panelHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                )
                .padding(horizontal = 24.dp),
        ) {
            Spacer(Modifier.height(readingLineOffset))
            Text(
                text = text,
                color = Color.White,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.3f).sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
            )
            // Lets the last line scroll all the way up to the reading line.
            Spacer(Modifier.height(panelHeight))
        }
        Text(
            "▶",
            color = Color(0xFFFFD54F),
            fontSize = 14.sp,
            modifier = Modifier
                .padding(start = 6.dp)
                .offset(y = readingLineOffset + (fontSizeSp * 0.65f - 7f).dp),
        )
    }
}
