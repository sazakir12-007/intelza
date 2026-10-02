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
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.ht.intelza.data.db.ClassEntity
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.domain.ClassProgress
import com.ht.intelza.domain.StudentProgress
import com.ht.intelza.export.ReportExporter
import com.ht.intelza.export.Sharing
import com.ht.intelza.ui.common.CardNumberBadge
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatPercent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class ClassReport(
    val sessions: List<SessionOverview>,
    val students: List<StudentProgress>,
)

class ClassReportViewModel(
    private val classes: ClassRepository,
    private val reports: ReportRepository,
    settings: SettingsRepository,
    private val exporter: ReportExporter,
    savedState: SavedStateHandle,
) : ViewModel() {
    val classId: Long = checkNotNull(savedState["classId"])

    val schoolClass: StateFlow<ClassEntity?> = classes.observeClass(classId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val report: StateFlow<ClassReport?> = combine(
        reports.observeSessionsForClass(classId),
        classes.observeStudents(classId),
    ) { sessions, _ -> sessions }
        .map { sessions ->
            // Oldest first, so trends read left to right.
            val ordered = sessions.sortedBy { it.startedAt }
            ClassReport(
                sessions = ordered,
                students = ClassProgress.compute(ordered, reports.getClassScores(classId), classes.getAllStudents(classId)),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun exportCsv(onReady: (File) -> Unit, onError: () -> Unit) = viewModelScope.launch {
        runCatching { exporter.classCsv(classId) }.onSuccess(onReady).onFailure { onError() }
    }
}

/** A class's results over time, with the students who need help first. */
@Composable
fun ClassReportScreen(
    onBack: () -> Unit,
    onOpenSession: (Long) -> Unit,
    onStudent: (Long) -> Unit,
) {
    val viewModel = appViewModel { c, s -> ClassReportViewModel(c.classes, c.reports, c.settings, c.exporter, s) }
    val schoolClass by viewModel.schoolClass.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportFailed = stringResource(R.string.export_failed)
    val shareTitle = stringResource(R.string.share_report)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(schoolClass?.name.orEmpty())
                        Text(
                            stringResource(R.string.class_report),
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
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.exportCsv(
                                onReady = { Sharing.share(context, it, "text/csv", shareTitle) },
                                onError = { scope.launch { snackbar.showSnackbar(exportFailed) } },
                            )
                        },
                        enabled = report?.sessions?.isNotEmpty() == true,
                    ) {
                        Icon(Icons.Outlined.Share, stringResource(R.string.share_gradebook))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val data = report ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (data.sessions.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.no_class_reports_title),
                        message = stringResource(R.string.no_class_reports_message),
                    )
                }
                return@LazyColumn
            }
            val averages = data.sessions.mapNotNull { it.average }
            val overall = averages.takeIf { it.isNotEmpty() }?.average()?.toFloat()
            item {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatBlock(
                        value = overall?.let(::formatPercent) ?: "–",
                        label = stringResource(R.string.class_average),
                        color = overall?.let { scoreColor(it, settings) } ?: MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    StatBlock(
                        value = data.sessions.size.toString(),
                        label = stringResource(R.string.evaluations),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                ScoreTrend(
                    scores = data.sessions.map { it.average },
                    settings = settings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }

            item { SectionHeader(stringResource(R.string.students_lowest_first)) }
            val students = data.students.sortedWith(compareBy(nullsLast<Float>()) { it.average })
            items(students, key = { "st-${it.student.id}" }) { progress ->
                ListItem(
                    modifier = Modifier.clickable { onStudent(progress.student.id) },
                    leadingContent = { CardNumberBadge(progress.student.cardNumber, size = 36.dp) },
                    headlineContent = { Text(progress.student.name) },
                    supportingContent = {
                        Text(
                            pluralStringResource(R.plurals.evaluation_count, progress.scores.size, progress.scores.size) +
                                if (!progress.student.active) " · " + stringResource(R.string.left_class) else "",
                        )
                    },
                    trailingContent = { ScoreChip(progress.average, settings) },
                )
                HorizontalDivider()
            }

            item { SectionHeader(stringResource(R.string.evaluations)) }
            items(data.sessions.asReversed(), key = { "se-${it.id}" }) { session ->
                SessionRow(session, settings, onClick = { onOpenSession(session.id) })
                HorizontalDivider()
            }
        }
    }
}
