package com.ht.intelza.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ht.intelza.data.AppSettings
import com.ht.intelza.ui.common.formatPercent
import com.ht.intelza.ui.theme.ResultColors

/** Green when on track, amber in between, red when the student or class needs help. */
fun scoreColor(fraction: Float, settings: AppSettings): Color {
    val percent = fraction * 100
    return when {
        percent < settings.needsHelpThreshold -> ResultColors.incorrect
        percent < settings.reteachThreshold -> ResultColors.warning
        else -> ResultColors.correct
    }
}

@Composable
fun ScoreChip(fraction: Float?, settings: AppSettings, modifier: Modifier = Modifier) {
    val color = fraction?.let { scoreColor(it, settings) } ?: ResultColors.noAnswer
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.15f), modifier = modifier) {
        Text(
            fraction?.let(::formatPercent) ?: "–",
            color = color,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Large figure with a caption, e.g. the class average. */
@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ReportCard(title: String, content: @Composable () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

/** A thin progress bar for a 0–1 fraction. */
@Composable
fun FractionBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(color),
        )
    }
}

/** Small column chart of scores over time (oldest first). */
@Composable
fun ScoreTrend(scores: List<Float?>, settings: AppSettings, modifier: Modifier = Modifier) {
    Row(
        modifier.height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        for (score in scores.takeLast(MAX_TREND)) {
            Box(
                Modifier
                    .weight(1f, fill = false)
                    .width(16.dp)
                    .fillMaxHeight((score ?: 0f).coerceIn(0.04f, 1f))
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(score?.let { scoreColor(it, settings) } ?: ResultColors.noAnswer),
            )
        }
    }
}

private const val MAX_TREND = 20
