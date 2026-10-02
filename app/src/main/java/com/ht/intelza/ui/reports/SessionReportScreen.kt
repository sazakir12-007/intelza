package com.ht.intelza.ui.reports

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SessionRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.domain.SessionReport
import com.ht.intelza.export.ReportExporter
import com.ht.intelza.export.Sharing
import com.ht.intelza.ui.common.AnswerBars
import com.ht.intelza.ui.common.CardNumberBadge
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDateTime
import com.ht.intelza.ui.common.formatPercent
import com.ht.intelza.ui.session.options
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class SessionReportViewModel(
    reports: ReportRepository,
    settings: SettingsRepository,
    private val sessions: SessionRepository,
    private val exporter: ReportExporter,
    savedState: SavedStateHandle,
) : ViewModel() {
    val sessionId: Long = checkNotNull(savedState["sessionId"])

    val overview: StateFlow<SessionOverview?> = reports.observeSessionOverview(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val report: StateFlow<SessionReport?> = reports.observeSessionReport(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun exportPdf(onReady: (File) -> Unit, onError: () -> Unit) = viewModelScope.launch {
        runCatching { exporter.sessionPdf(sessionId) }.onSuccess(onReady).onFailure { onError() }
    }

    fun exportCsv(onReady: (File) -> Unit, onError: () -> Unit) = viewModelScope.launch {
        runCatching { exporter.sessionCsv(sessionId) }.onSuccess(onReady).onFailure { onError() }
    }

    fun reopen(onReady: () -> Unit) = viewModelScope.launch {
        sessions.reopen(sessionId)
        onReady()
    }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        sessions.deleteSession(sessionId)
        onDeleted()
    }
}

@Composable
fun SessionReportScreen(
    onBack: () -> Unit,
    onContinue: (sessionId: Long) -> Unit,
    onStudent: (studentId: Long) -> Unit,
) {
    val viewModel = appViewModel { c, s -> SessionReportViewModel(c.reports, c.settings, c.sessions, c.exporter, s) }
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val report by viewModel.report.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val exportFailed = stringResource(R.string.export_failed)
    val shareTitle = stringResource(R.string.share_report)
    var shareMenu by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var expandedQuestion by rememberSaveable { mutableStateOf<Long?>(null) }

    fun share(file: File, mime: String) {
        Sharing.share(context, file, mime, shareTitle)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            overview?.title?.ifEmpty { stringResource(R.string.quick_session) }.orEmpty(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        overview?.let {
                            Text(
                                "${it.className} · ${formatDateTime(it.startedAt)}",
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
                actions = {
                    Box {
                        IconButton(onClick = { shareMenu = true }) {
                            Icon(Icons.Outlined.Share, stringResource(R.string.share_report))
                        }
                        DropdownMenu(expanded = shareMenu, onDismissRequest = { shareMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_pdf_report)) },
                                leadingIcon = { Icon(Icons.Outlined.PictureAsPdf, null) },
                                onClick = {
                                    shareMenu = false
                                    viewModel.exportPdf(
                                        onReady = { share(it, "application/pdf") },
                                        onError = { scope.launch { snackbar.showSnackbar(exportFailed) } },
                                    )
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.share_csv_report)) },
                                leadingIcon = { Icon(Icons.Outlined.TableChart, null) },
                                onClick = {
                                    shareMenu = false
                                    viewModel.exportCsv(
                                        onReady = { share(it, "text/csv") },
                                        onError = { scope.launch { snackbar.showSnackbar(exportFailed) } },
                                    )
                                },
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.continue_evaluation)) },
                                leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) },
                                onClick = {
                                    menu = false
                                    viewModel.reopen { onContinue(viewModel.sessionId) }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.delete_evaluation)) },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                                onClick = {
                                    menu = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val data = report ?: return@Scaffold
        val info = overview ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Summary(info, data, settings) }
            item { Flags(data, settings, onStudent) }

            item { SectionHeader(stringResource(R.string.students)) }
            items(data.students, key = { "s-${it.student.studentId}" }) { result ->
                ListItem(
                    modifier = Modifier.clickable { onStudent(result.student.studentId) },
                    leadingContent = { CardNumberBadge(result.student.cardNumber, size = 36.dp) },
                    headlineContent = { Text(result.student.name) },
                    supportingContent = {
                        Text(stringResource(R.string.correct_of, result.correct, result.scoredQuestions))
                    },
                    trailingContent = { ScoreChip(result.score, settings) },
                )
                HorizontalDivider()
            }
            if (data.absent.isNotEmpty()) {
                item {
                    Text(
                        stringResource(R.string.absent_list, data.absent.joinToString(", ") { it.name }),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            item { SectionHeader(stringResource(R.string.questions)) }
            itemsIndexed(data.questions, key = { _, q -> "q-${q.question.id}" }) { index, result ->
                val expanded = expandedQuestion == result.question.id
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clickable { expandedQuestion = if (expanded) null else result.question.id }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Q${index + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(40.dp))
                        Text(
                            result.question.text.ifBlank { stringResource(R.string.picture_question) },
                            maxLines = if (expanded) 6 else 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (result.isScored) {
                            ScoreChip(result.fractionCorrect, settings)
                        } else {
                            Text(stringResource(R.string.type_poll), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    AnimatedVisibility(expanded) {
                        AnswerBars(
                            options = result.question.options,
                            counts = result.counts,
                            total = result.present,
                            correct = result.question.correct,
                            noAnswerCount = result.noAnswer.size,
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_evaluation_title),
            message = stringResource(R.string.delete_evaluation_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onBack)
            },
        )
    }
}

@Composable
private fun Summary(info: SessionOverview, report: SessionReport, settings: AppSettings) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val average = report.classAverage
        StatBlock(
            value = average?.let(::formatPercent) ?: "–",
            label = stringResource(R.string.class_average),
            color = average?.let { scoreColor(it, settings) } ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        StatBlock(
            value = "${report.students.size}/${report.students.size + report.absent.size}",
            label = stringResource(R.string.students_present),
            modifier = Modifier.weight(1f),
        )
        StatBlock(
            value = report.questions.size.toString(),
            label = stringResource(R.string.questions_asked),
            modifier = Modifier.weight(1f),
        )
    }
    if (info.finishedAt == null) {
        Text(
            stringResource(R.string.evaluation_in_progress),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Flags(report: SessionReport, settings: AppSettings, onStudent: (Long) -> Unit) {
    val struggling = report.studentsNeedingHelp(settings.needsHelpThreshold)
    val reteach = report.questionsToReteach(settings.reteachThreshold)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportCard(stringResource(R.string.needs_help_heading, settings.needsHelpThreshold)) {
            if (struggling.isEmpty()) {
                Text(stringResource(R.string.nobody_needs_help), style = MaterialTheme.typography.bodyMedium)
            }
            for (result in struggling) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onStudent(result.student.studentId) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(result.student.name, modifier = Modifier.weight(1f))
                    ScoreChip(result.score, settings)
                }
            }
        }
        ReportCard(stringResource(R.string.reteach_heading, settings.reteachThreshold)) {
            if (reteach.isEmpty()) {
                Text(stringResource(R.string.nothing_to_reteach), style = MaterialTheme.typography.bodyMedium)
            }
            for (result in reteach) {
                val number = report.questions.indexOf(result) + 1
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Q$number · " + result.question.text.ifBlank { stringResource(R.string.picture_question) },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    ScoreChip(result.fractionCorrect, settings)
                }
            }
        }
    }
}
