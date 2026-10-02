package com.ht.intelza.ui.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ht.intelza.R
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.ui.common.formatPercent
import com.ht.intelza.ui.questions.StoredImage
import com.ht.intelza.ui.questions.optionText
import com.ht.intelza.ui.theme.AnswerColors
import com.ht.intelza.ui.theme.ResultColors

private val PresenterBackground = Brush.linearGradient(listOf(Color(0xFF151A3D), Color(0xFF26338A)))
private val Faded = Color.White.copy(alpha = 0.7f)

/**
 * Full-screen view for a TV or projector (requirement E1). Sizes scale with the screen so
 * text stays readable from the back of the room. It never shows who answered what.
 */
@Composable
fun PresenterContent(state: PresenterState?, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(PresenterBackground),
    ) {
        // 1.0 on a 960x540 dp screen; larger screens scale everything up.
        val scale = minOf(maxWidth / 960.dp, maxHeight / 540.dp).coerceIn(0.5f, 3f)
        val s = Scale(scale)
        if (state == null) return@BoxWithConstraints
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = s.dp(40), vertical = s.dp(28)),
        ) {
            Header(state, s)
            Spacer(Modifier.height(s.dp(16)))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (state.stage) {
                    PresenterStage.WAITING -> Waiting(state, s)
                    PresenterStage.QUESTION -> QuestionView(state, s)
                    PresenterStage.COLLECTING -> Collecting(state, s)
                    PresenterStage.RESULTS -> Results(state, s)
                }
            }
        }
    }
}

private class Scale(val factor: Float) {
    fun dp(value: Int): Dp = (value * factor).dp
    fun sp(value: Int): TextUnit = (value * factor).sp
}

@Composable
private fun Header(state: PresenterState, s: Scale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            listOf(state.className, state.title.ifEmpty { stringResource(R.string.quick_session) })
                .filter { it.isNotBlank() }
                .joinToString(" · "),
            color = Faded,
            fontSize = s.sp(18),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (state.questionNumber > 0) {
            Text(
                stringResource(R.string.question_x_of_y, state.questionNumber, state.questionCount),
                color = Faded,
                fontSize = s.sp(18),
            )
        }
    }
}

@Composable
private fun Waiting(state: PresenterState, s: Scale) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            state.title.ifEmpty { stringResource(R.string.quick_session) },
            color = Color.White,
            fontSize = s.sp(52),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(s.dp(16)))
        Text(
            stringResource(R.string.presenter_get_ready),
            color = Faded,
            fontSize = s.sp(26),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun QuestionView(state: PresenterState, s: Scale) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(s.dp(32))) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            if (state.questionText.isNotBlank()) {
                Text(
                    state.questionText,
                    color = Color.White,
                    fontSize = s.sp(if (state.questionText.length > 120) 30 else 40),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = s.sp(if (state.questionText.length > 120) 38 else 50),
                )
                Spacer(Modifier.height(s.dp(28)))
            }
            for ((option, text) in state.options) {
                OptionLine(option, optionText(state.type, option, text), s)
                Spacer(Modifier.height(s.dp(12)))
            }
        }
        state.imageName?.let { name ->
            StoredImage(
                name = name,
                maxSize = 1600,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(0.8f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(s.dp(16))),
            )
        }
    }
}

@Composable
private fun OptionLine(option: AnswerOption, text: String, s: Scale) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LetterBox(option, s, size = 44)
        if (text.isNotBlank()) {
            Spacer(Modifier.width(s.dp(16)))
            Text(text, color = Color.White, fontSize = s.sp(28), maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun LetterBox(option: AnswerOption, s: Scale, size: Int) {
    Box(
        Modifier
            .size(s.dp(size))
            .clip(RoundedCornerShape(s.dp(10)))
            .background(AnswerColors.of(option)),
        contentAlignment = Alignment.Center,
    ) {
        Text(option.name, color = Color.White, fontSize = s.sp(size / 2 + 4), fontWeight = FontWeight.Bold)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Collecting(state: PresenterState, s: Scale) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.questionText.isNotBlank()) {
            Text(
                state.questionText,
                color = Faded,
                fontSize = s.sp(22),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(s.dp(12)))
        }
        Text(
            stringResource(R.string.presenter_answered, state.answeredCount, state.presentCount),
            color = Color.White,
            fontSize = s.sp(48),
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(s.dp(20)))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(s.dp(10), Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(s.dp(10)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            val chipStyle = TextStyle(fontSize = s.sp(if (state.roster.size > 30) 15 else 19))
            for ((name, answered) in state.roster) {
                val shape = RoundedCornerShape(50)
                Box(
                    Modifier
                        .clip(shape)
                        .background(if (answered) ResultColors.correct else Color.Transparent)
                        .border(s.dp(2), if (answered) ResultColors.correct else Faded, shape)
                        .padding(horizontal = s.dp(14), vertical = s.dp(6)),
                ) {
                    Text(
                        if (answered) "✓ $name" else name,
                        color = Color.White,
                        style = chipStyle,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(state: PresenterState, s: Scale) {
    Column(Modifier.fillMaxSize()) {
        if (state.questionText.isNotBlank()) {
            Text(
                state.questionText,
                color = Color.White,
                fontSize = s.sp(26),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(s.dp(16)))
        }
        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(s.dp(36))) {
            // Vertical bars, relative to the number of students present.
            Row(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(s.dp(24), Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                for ((option, text) in state.options) {
                    val count = state.counts[option] ?: 0
                    val fraction = if (state.presentCount > 0) count.toFloat() / state.presentCount else 0f
                    val isCorrect = option == state.correct
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(count.toString(), color = Color.White, fontSize = s.sp(30), fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(s.dp(6)))
                        Box(
                            Modifier
                                .fillMaxWidth(0.8f)
                                .fillMaxHeight(fraction.coerceIn(0.02f, 1f) * 0.75f)
                                .clip(RoundedCornerShape(topStart = s.dp(10), topEnd = s.dp(10)))
                                .background(AnswerColors.of(option).copy(alpha = if (state.correct == null || isCorrect) 1f else 0.45f)),
                        )
                        Spacer(Modifier.height(s.dp(10)))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LetterBox(option, s, size = 40)
                            if (isCorrect) {
                                Text(" ✓", color = ResultColors.correct, fontSize = s.sp(30), fontWeight = FontWeight.Bold)
                            }
                        }
                        val label = optionText(state.type, option, text)
                        if (label.isNotBlank()) {
                            Text(
                                label,
                                color = Faded,
                                fontSize = s.sp(16),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            state.fractionCorrect?.let { fraction ->
                Column(
                    Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        formatPercent(fraction),
                        color = Color.White,
                        fontSize = s.sp(64),
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.got_it_right), color = Faded, fontSize = s.sp(22))
                }
            }
        }
    }
}
