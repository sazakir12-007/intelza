package com.ht.intelza.ui.common

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ht.intelza.R
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.ui.theme.AnswerColors
import com.ht.intelza.ui.theme.ResultColors

/**
 * Horizontal bars showing how many students chose each option. Bar length is relative to
 * the number of students who could answer, so a full bar means everyone chose it.
 */
@Composable
fun AnswerBars(
    options: List<AnswerOption>,
    counts: Map<AnswerOption, Int>,
    total: Int,
    correct: AnswerOption?,
    modifier: Modifier = Modifier,
    noAnswerCount: Int? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (option in options) {
            val count = counts[option] ?: 0
            BarRow(
                label = { LetterTile(option.name, AnswerColors.of(option)) },
                fraction = if (total > 0) count.toFloat() / total else 0f,
                color = AnswerColors.of(option),
                count = count,
                isCorrect = option == correct,
            )
        }
        if (noAnswerCount != null && noAnswerCount > 0) {
            BarRow(
                label = { LetterTile("–", ResultColors.noAnswer) },
                fraction = if (total > 0) noAnswerCount.toFloat() / total else 0f,
                color = ResultColors.noAnswer,
                count = noAnswerCount,
                isCorrect = false,
                caption = stringResource(R.string.no_answer),
            )
        }
    }
}

@Composable
private fun BarRow(
    label: @Composable () -> Unit,
    fraction: Float,
    color: androidx.compose.ui.graphics.Color,
    count: Int,
    isCorrect: Boolean,
    caption: String? = null,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        label()
        Box(
            Modifier
                .weight(1f)
                .height(22.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(8.dp))
                    .background(color),
            )
            if (caption != null) {
                Text(
                    caption,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        Text(
            count.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
        )
        Box(Modifier.width(24.dp)) {
            if (isCorrect) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = stringResource(R.string.correct_answer),
                    tint = ResultColors.correct,
                )
            }
        }
    }
}
