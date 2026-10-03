package com.ht.intelza.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.ui.common.BrandLockup
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDate
import com.ht.intelza.ui.reports.SessionRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.DrawerMenuButton
import com.ht.intelza.ui.common.CompactListItem

data class HomeState(
    val inProgress: List<SessionOverview>,
    val recent: List<SessionOverview>,
    val hasClasses: Boolean,
    val hasQuestions: Boolean,
    val settings: AppSettings,
)

class HomeViewModel(
    reports: ReportRepository,
    classes: ClassRepository,
    questions: QuestionRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<HomeState?> = combine(
        reports.observeSessions(),
        classes.observeSummaries(),
        questions.observeSubjects(),
        settings.settings,
    ) { sessions, classList, subjects, appSettings ->
        HomeState(
            inProgress = sessions.filter { it.finishedAt == null },
            recent = sessions.filter { it.finishedAt != null }.take(RECENT_COUNT),
            hasClasses = classList.any { it.studentCount > 0 },
            hasQuestions = subjects.any { it.questionCount > 0 },
            settings = appSettings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private companion object {
        const val RECENT_COUNT = 5
    }
}

@Composable
fun HomeScreen(
    onStartEvaluation: () -> Unit,
    onContinueSession: (Long) -> Unit,
    onOpenReport: (Long) -> Unit,
    onPrintCards: () -> Unit,
    onTestCards: () -> Unit,
    onSettings: () -> Unit,
    onOpenClasses: () -> Unit,
    onOpenQuestions: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> HomeViewModel(c.reports, c.classes, c.questions, c.settings) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { BrandLockup() },
                navigationIcon = { DrawerMenuButton() },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
    ) { padding ->
        val home = state ?: return@Scaffold
        LazyColumn(
            Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            stringResource(R.string.home_tagline),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Button(onClick = onStartEvaluation, enabled = home.hasClasses) {
                            Icon(Icons.Outlined.PlayArrow, null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.start_evaluation))
                        }
                    }
                }
            }

            if (!home.hasClasses || home.recent.isEmpty() && home.inProgress.isEmpty()) {
                item {
                    GettingStarted(
                        hasClasses = home.hasClasses,
                        hasQuestions = home.hasQuestions,
                        onClasses = onOpenClasses,
                        onPrintCards = onPrintCards,
                        onQuestions = onOpenQuestions,
                    )
                }
            }

            if (home.inProgress.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.continue_evaluation)) }
                items(home.inProgress, key = { "p-${it.id}" }) { session ->
                    CompactListItem(
                        modifier = Modifier.clickable { onContinueSession(session.id) },
                        leadingContent = { Icon(Icons.Outlined.PlayCircle, null, tint = MaterialTheme.colorScheme.tertiary) },
                        headlineContent = { Text(session.title.ifEmpty { stringResource(R.string.quick_session) }) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.session_progress,
                                    session.className,
                                    session.askedQuestions,
                                    session.totalQuestions,
                                    formatDate(session.startedAt),
                                ),
                            )
                        },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    )
                    HorizontalDivider()
                }
            }

            if (home.recent.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.recent_evaluations)) }
                items(home.recent, key = { "r-${it.id}" }) { session ->
                    SessionRow(session, home.settings, onClick = { onOpenReport(session.id) })
                    HorizontalDivider()
                }
            }

            item { SectionHeader(stringResource(R.string.answer_cards)) }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ToolCard(Icons.Outlined.Print, stringResource(R.string.print_cards), onPrintCards, Modifier.weight(1f))
                    ToolCard(Icons.Outlined.QrCodeScanner, stringResource(R.string.test_cards_title), onTestCards, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun GettingStarted(
    hasClasses: Boolean,
    hasQuestions: Boolean,
    onClasses: () -> Unit,
    onPrintCards: () -> Unit,
    onQuestions: () -> Unit,
) {
    OutlinedCard(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
    ) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                stringResource(R.string.getting_started),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            Step(1, stringResource(R.string.step_add_class), stringResource(R.string.step_add_class_hint), hasClasses, onClasses)
            Step(2, stringResource(R.string.step_print_cards), stringResource(R.string.step_print_cards_hint), false, onPrintCards)
            Step(3, stringResource(R.string.step_add_questions), stringResource(R.string.step_add_questions_hint), hasQuestions, onQuestions)
        }
    }
}

@Composable
private fun Step(number: Int, title: String, hint: String, done: Boolean, onClick: () -> Unit) {
    CompactListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = if (done) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(32.dp),
            ) {
                Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (done) "✓" else number.toString(),
                        color = if (done) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(hint) },
    )
}

@Composable
private fun ToolCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(onClick = onClick, modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.titleSmall)
        }
    }
}
