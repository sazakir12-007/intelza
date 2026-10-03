package com.ht.intelza.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.data.db.TopicQuestionStat
import com.ht.intelza.data.db.TopicWithSubject
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import com.ht.intelza.ui.common.AppTopBar

class TopicReportViewModel(
    questions: QuestionRepository,
    reports: ReportRepository,
    settings: SettingsRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val topicId: Long = checkNotNull(savedState["topicId"])

    val topic: StateFlow<TopicWithSubject?> = questions.observeTopic(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val sessions: StateFlow<List<SessionOverview>?> = reports.observeSessionsForTopic(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val questionStats: StateFlow<List<TopicQuestionStat>?> = reports.observeTopicQuestionStats(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

/** How a topic went across every class and session that covered it (requirement F4). */
@Composable
fun TopicReportScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val viewModel = appViewModel { c, s -> TopicReportViewModel(c.questions, c.reports, c.settings, s) }
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val stats by viewModel.questionStats.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(topic?.topic?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            stringResource(R.string.topic_report),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        val sessionList = sessions ?: return@Scaffold
        val questionList = stats ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (sessionList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.no_topic_reports_title),
                        message = stringResource(R.string.no_topic_reports_message),
                    )
                }
                return@LazyColumn
            }
            item { SectionHeader(stringResource(R.string.evaluations)) }
            items(sessionList, key = { "s-${it.id}" }) { session ->
                SessionRow(session, settings, onClick = { onOpenSession(session.id) })
                HorizontalDivider()
            }
            item { SectionHeader(stringResource(R.string.questions)) }
            items(questionList, key = { "q-${it.questionKey}" }) { stat ->
                val fraction = if (stat.possibleAnswers > 0) stat.correctAnswers.toFloat() / stat.possibleAnswers else null
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stat.text.ifBlank { stringResource(R.string.picture_question) },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            pluralStringResource(R.plurals.times_asked, stat.timesAsked, stat.timesAsked),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ScoreChip(fraction, settings)
                }
                HorizontalDivider()
            }
        }
    }
}
