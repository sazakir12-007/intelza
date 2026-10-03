package com.ht.intelza.ui.session

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ht.intelza.R
import com.ht.intelza.data.db.SessionStudentRow
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.ui.scan.CameraPermissionGate
import com.ht.intelza.ui.scan.CardMarker
import com.ht.intelza.ui.scan.ScannerView
import com.ht.intelza.ui.scan.rememberTagScanner
import com.ht.intelza.ui.theme.ResultColors

/**
 * Full-screen scanner for one question (requirements D3, D4, D10). Cards from other
 * classes are ignored; each student's answer is saved once it is stable.
 */
@Composable
fun SessionScanView(
    state: SessionUiState,
    recordedByCard: () -> Map<Int, AnswerOption>,
    onConfirmed: (Map<Int, AnswerOption>) -> Unit,
    onStudentTapped: (SessionStudentRow) -> Unit,
    onDone: () -> Unit,
) {
    CameraPermissionGate {
        val scanner = rememberTagScanner()
        val question = state.current
        LaunchedEffect(scanner, question?.id) {
            scanner.acceptedCards = state.cardNumbers
            scanner.reset(recordedByCard())
        }

        val view = LocalView.current
        val settings by rememberUpdatedState(state.settings)
        val confirmed by rememberUpdatedState(onConfirmed)
        val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 70) }.getOrNull() }
        DisposableEffect(tone) { onDispose { tone?.release() } }
        LaunchedEffect(scanner) {
            scanner.confirmations.collect { changes ->
                confirmed(changes)
                if (settings.scanSound) tone?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
                if (settings.scanVibration) {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            HapticFeedbackConstants.CONFIRM
                        } else {
                            HapticFeedbackConstants.VIRTUAL_KEY
                        },
                    )
                }
            }
        }

        var showNames by rememberSaveable { mutableStateOf(state.settings.showNamesWhileScanning) }
        var zoom by remember { mutableFloatStateOf(1f) }
        val studentsByCard = state.students.associateBy { it.cardNumber }
        val answers = state.currentAnswers
        val answersByCard = state.students.mapNotNull { s -> answers[s.studentId]?.let { s.cardNumber to it } }.toMap()
        val present = state.presentStudents
        val missing = present.filter { it.studentId !in answers }

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            ScannerView(
                scanner = scanner,
                marker = { card ->
                    val answer = card.confirmed ?: answersByCard[card.cardNumber]
                    val name = studentsByCard[card.cardNumber]?.name ?: "#${card.cardNumber}"
                    if (answer != null) {
                        CardMarker(
                            label = if (showNames) "$name · ${answer.letter}" else "✓",
                            color = ResultColors.correct,
                        )
                    } else {
                        CardMarker(label = if (showNames) name else null, color = Color.White)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Top: question and counter.
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                ) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                        Text(
                            stringResource(R.string.question_x_of_y, state.currentIndex + 1, state.questions.size),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        question?.text?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                FilledTonalIconButton(onClick = { showNames = !showNames }) {
                    Icon(
                        if (showNames) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                        contentDescription = stringResource(
                            if (showNames) R.string.hide_names else R.string.show_names,
                        ),
                    )
                }
            }

            // Bottom: progress, missing students and controls.
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.answered_count, present.size - missing.size, present.size),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        for (ratio in listOf(1f, 2f, 3f)) {
                            FilterChip(
                                selected = zoom == ratio,
                                onClick = {
                                    zoom = ratio
                                    scanner.setZoomRatio(ratio)
                                },
                                label = { Text("${ratio.toInt()}×") },
                                modifier = Modifier.padding(start = 4.dp),
                            )
                        }
                    }
                    if (missing.isNotEmpty()) {
                        Text(
                            stringResource(R.string.still_waiting_for),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            for (student in missing) {
                                SuggestionChip(
                                    onClick = { onStudentTapped(student) },
                                    label = { Text(student.name, maxLines = 1) },
                                )
                            }
                        }
                    } else if (present.isNotEmpty()) {
                        Text(
                            stringResource(R.string.everyone_answered),
                            style = MaterialTheme.typography.bodyMedium,
                            color = ResultColors.correct,
                        )
                    }
                    Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.done_scanning))
                    }
                }
            }
        }
    }
}
