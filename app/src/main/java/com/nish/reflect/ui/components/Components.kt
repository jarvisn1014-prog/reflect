package com.nish.reflect.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nish.reflect.ui.theme.Accent
import com.nish.reflect.ui.theme.AccentWarm
import com.nish.reflect.ui.theme.EmotionSlate

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(25.dp),
        color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SentimentPill(
    emotion: String,
    isCorrected: Boolean = false,
    onLongClick: () -> Unit = {}
) {
    val color = if (isCorrected) Color.Transparent else AccentWarm
    val textColor = if (isCorrected) AccentWarm else MaterialTheme.colorScheme.onPrimary
    val borderColor = if (isCorrected) AccentWarm else Color.Transparent

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick)
    ) {
        Text(
            text = emotion,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun ThemePill(label: String, count: Int) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = "· $count", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeeklyMoodChart(
    dayLabels: List<String>,
    dayValences: List<Float?>,
    modifier: Modifier = Modifier
) {
    val barWidth = 28.dp
    val barSpacing = 12.dp
    val maxBarHeight = 100.dp
    val primary = MaterialTheme.colorScheme.primary
    val slate = EmotionSlate

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(maxBarHeight + 24.dp)
        ) {
            val totalWidth = size.width
            val barWidthPx = barWidth.toPx()
            val spacingPx = barSpacing.toPx()
            val totalBarsWidth = 7 * barWidthPx + 6 * spacingPx
            val startX = (totalWidth - totalBarsWidth) / 2

            dayValences.forEachIndexed { index, valence ->
                val x = startX + index * (barWidthPx + spacingPx)
                val valenceSafe = valence ?: 0f
                // Map -1..1 to 0..1 for height
                val normalizedHeight = ((valenceSafe + 1f) / 2f).coerceIn(0f, 1f)
                val barHeight = normalizedHeight * maxBarHeight.toPx()
                val y = size.height - 24 - barHeight

                // Color: low mood = slate, high mood = primary
                val barColor = when {
                    valence == null -> slate.copy(alpha = 0.3f)
                    valenceSafe > 0.3f -> primary
                    valenceSafe > -0.1f -> primary.copy(alpha = 0.6f)
                    else -> slate
                }

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )

                // Peak marker dot
                if (valence != null && valence == dayValences.filterNotNull().maxOrNull()) {
                    drawCircle(
                        color = primary,
                        radius = 3f,
                        center = Offset(x + barWidthPx / 2, y - 6f)
                    )
                }
            }
        }
        // Day labels row below chart
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ProgressStrip() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        val transition = rememberInfiniteTransition(label = "progress")
        val alphas = (0..2).map { index ->
            transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "step$index"
            )
        }
        alphas.forEach { alpha ->
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .alpha(alpha.value)
                    .background(Accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "Reflecting...",
            style = MaterialTheme.typography.labelMedium,
            color = Accent
        )
    }
}

@Composable
fun EmptyState(title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}