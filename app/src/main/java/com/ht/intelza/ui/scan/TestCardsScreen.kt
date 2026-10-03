package com.ht.intelza.ui.scan

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.R
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.ui.theme.AnswerColors
import com.ht.intelza.ui.theme.ResultColors

/**
 * Lets the teacher check printed cards: every card in view is outlined with its number
 * and the answer it currently shows.
 */
@Composable
fun TestCardsScreen(onBack: () -> Unit) {
    CameraPermissionGate {
        val scanner = rememberTagScanner()
        val confirmed = remember { mutableStateMapOf<Int, AnswerOption>() }
        LaunchedEffect(scanner) {
            scanner.confirmations.collect { confirmed.putAll(it) }
        }
        val frame by scanner.frames.collectAsStateWithLifecycle()
        var zoom by remember { mutableFloatStateOf(1f) }

        Box(Modifier.fillMaxSize().background(Color.Black)) {
            ScannerView(
                scanner = scanner,
                marker = { card ->
                    val answer = card.reading ?: card.confirmed
                    CardMarker(
                        label = "#${card.cardNumber}  ${answer?.letter ?: "?"}",
                        color = if (card.reading != null) answer?.let(AnswerColors::of) ?: ResultColors.noAnswer
                        else ResultColors.noAnswer,
                    )
                },
                modifier = Modifier.fillMaxSize(),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                }
                Surface(shape = RoundedCornerShape(50), color = Color.Black.copy(alpha = 0.55f)) {
                    Text(
                        stringResource(R.string.test_cards_title),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            ) {
                Column(
                    Modifier
                        .navigationBarsPadding()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        stringResource(R.string.test_cards_hint),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for ((card, answer) in confirmed.toSortedMap()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = AnswerColors.of(answer),
                            ) {
                                Text(
                                    "#$card ${answer.letter}",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        for (ratio in listOf(1f, 2f, 3f)) {
                            FilterChip(
                                selected = zoom == ratio,
                                onClick = {
                                    zoom = ratio
                                    scanner.setZoomRatio(ratio)
                                },
                                label = { Text("${ratio.toInt()}×") },
                            )
                        }
                        Text(
                            frame?.let {
                                stringResource(
                                    R.string.test_cards_stats,
                                    it.framesPerSecond,
                                    it.processingMillis,
                                    it.imageSize.width,
                                    it.imageSize.height,
                                )
                            }.orEmpty(),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = {
                            confirmed.clear()
                            scanner.reset()
                        }) {
                            Text(stringResource(R.string.clear))
                        }
                    }
                }
            }
        }
    }
}
