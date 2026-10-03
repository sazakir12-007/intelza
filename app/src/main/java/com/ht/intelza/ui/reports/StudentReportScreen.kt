package com.ht.intelza.ui.reports

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.data.db.StudentSessionRow
import com.ht.intelza.domain.ClassProgress
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDate
import com.ht.intelza.ui.common.formatPercent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.CompactListItem

class StudentReportViewModel(
    classes: ClassRepository,
    reports: ReportRepository,
    settings: SettingsRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    private val studentId: Long = checkNotNull(savedState["studentId"])

    val student: StateFlow<StudentEntity?> = classes.observeStudent(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val history: StateFlow<List<StudentSessionRow>?> = reports.observeStudentHistory(studentId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _className = MutableStateFlow("")
    val className: StateFlow<String> = _className.asStateFlow()

    init {
        viewModelScope.launch {
            val s = classes.observeStudent(studentId).filterNotNull().first()
            _className.value = classes.getClass(s.classId)?.name.orEmpty()
        }
    }
}

@Composable
fun StudentReportScreen(onBack: () -> Unit, onOpenSession: (Long) -> Unit) {
    val viewModel = appViewModel { c, s -> StudentReportViewModel(c.classes, c.reports, c.settings, s) }
    val student by viewModel.student.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val className by viewModel.className.collectAsStateWithLifecycle()
    val quickLabel = stringResource(R.string.quick_session)

    Scaffold(
        topBar = {
            AppTopBar(
                title = {
                    Column {
                        Text(student?.name.orEmpty())
                        if (className.isNotEmpty()) {
                            Text(
                                className,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
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
        val rows = history ?: return@Scaffold
        val overall = ClassProgress.overallAverage(rows)
        val subjects = ClassProgress.subjectAverages(rows, quickLabel)
        val attended = rows.count { it.present }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            if (rows.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.no_student_history_title),
                        message = stringResource(R.string.no_student_history_message),
                    )
                }
                return@LazyColumn
            }
            item {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatBlock(
                        value = overall?.let(::formatPercent) ?: "–",
                        label = stringResource(R.string.overall_average),
                        color = overall?.let { scoreColor(it, settings) } ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    StatBlock(
                        value = "$attended/${rows.size}",
                        label = stringResource(R.string.evaluations_attended),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Column(Modifier.padding(horizontal = 12.dp)) {
                    ScoreTrend(
                        scores = rows.map { row ->
                            if (row.present && row.scoredQuestions > 0) row.correctAnswers.toFloat() / row.scoredQuestions else null
                        },
                        settings = settings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (subjects.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.by_subject)) }
                items(subjects, key = { "subject-${it.subject}" }) { subject ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(subject.subject, style = MaterialTheme.typography.bodyLarge)
                            FractionBar(subject.average, scoreColor(subject.average, settings), Modifier.fillMaxWidth())
                            Text(
                                pluralStringResource(R.plurals.evaluation_count, subject.sessions, subject.sessions),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        ScoreChip(subject.average, settings)
                    }
                }
            }
            item { SectionHeader(stringResource(R.string.history)) }
            items(rows.asReversed(), key = { "session-${it.sessionId}" }) { row ->
                CompactListItem(
                    modifier = Modifier.clickable { onOpenSession(row.sessionId) },
                    headlineContent = { Text(row.title.ifEmpty { quickLabel }) },
                    supportingContent = {
                        Text(
                            listOfNotNull(row.subjectName.takeIf { it.isNotBlank() }, formatDate(row.startedAt))
                                .joinToString(" · "),
                        )
                    },
                    trailingContent = {
                        if (row.present) {
                            ScoreChip(
                                if (row.scoredQuestions > 0) row.correctAnswers.toFloat() / row.scoredQuestions else null,
                                settings,
                            )
                        } else {
                            Text(stringResource(R.string.absent), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                )
                HorizontalDivider()
            }
        }
    }
}
