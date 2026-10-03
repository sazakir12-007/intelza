package com.ht.intelza.ui.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.HowToReg
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.R
import com.ht.intelza.data.db.SessionStudentRow
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.AppTopBar

@Composable
fun SessionScreen(onBack: () -> Unit, onFinished: (sessionId: Long) -> Unit) {
    val viewModel = appViewModel { c, s -> SessionViewModel(c.sessions, c.classes, c.settings, s) }
    val loaded by viewModel.state.collectAsStateWithLifecycle()
    val presenterState by viewModel.presenter.collectAsStateWithLifecycle()
    val externalDisplay = SecondaryDisplayPresenter(presenterState)

    var presenting by rememberSaveable { mutableStateOf(false) }
    var showAttendance by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var manualFor by remember { mutableStateOf<SessionStudentRow?>(null) }

    // An evaluation can take a while; keep the phone awake meanwhile.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val state = loaded
    if (state == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val phase = state.phase
    val finish = { viewModel.finish { onFinished(viewModel.sessionId) } }

    BackHandler(enabled = phase == SessionPhase.SCANNING) { viewModel.stopScan() }
    BackHandler(enabled = presenting && phase != SessionPhase.SCANNING) { presenting = false }

    when {
        phase == SessionPhase.SCANNING -> SessionScanView(
            state = state,
            recordedByCard = viewModel::recordedAnswersByCard,
            onConfirmed = viewModel::onCardsConfirmed,
            onStudentTapped = { manualFor = it },
            onDone = viewModel::stopScan,
        )

        presenting -> PresenterMode(
            presenterState = presenterState,
            phase = phase,
            revealed = state.revealed,
            onExit = { presenting = false },
            onScan = viewModel::startScan,
            onReveal = viewModel::setRevealed,
            onNext = viewModel::next,
        )

        else -> Scaffold(
            topBar = {
                AppTopBar(
                    title = {
                        Column {
                            Text(state.className, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                state.session.title.ifEmpty { stringResource(R.string.quick_session) },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                        }
                    },
                    actions = {
                        IconButton(onClick = { presenting = true }) {
                            Icon(Icons.Outlined.Slideshow, stringResource(R.string.present_on_screen))
                        }
                        IconButton(onClick = { showAttendance = true }) {
                            Icon(Icons.Outlined.HowToReg, stringResource(R.string.attendance))
                        }
                        SessionMenu(
                            canFinish = state.questions.any { it.status == com.ht.intelza.domain.QuestionStatus.ASKED },
                            onFinish = finish,
                            onDelete = { confirmDelete = true },
                        )
                    },
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .padding(padding)
                    .fillMaxSize(),
            ) {
                externalDisplay?.let {
                    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Outlined.Tv, null)
                            Text(stringResource(R.string.showing_on_display, it), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                QuestionStrip(state, onSelect = viewModel::goTo)
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                        .navigationBarsPadding(),
                ) {
                    when (phase) {
                        SessionPhase.QUESTION -> QuestionPanel(
                            question = state.current!!,
                            onScan = viewModel::startScan,
                            onSkip = viewModel::skip,
                        )
                        SessionPhase.RESULTS -> ResultsPanel(
                            state = state,
                            presenting = externalDisplay != null,
                            onScanAgain = viewModel::startScan,
                            onSetCorrect = viewModel::setCorrectAnswer,
                            onStudentTapped = { manualFor = it },
                            onReveal = viewModel::setRevealed,
                            onNext = viewModel::next,
                        )
                        SessionPhase.WRAP_UP -> WrapUpPanel(
                            state = state,
                            onAskQuick = viewModel::askQuickQuestion,
                            onFinish = finish,
                        )
                        SessionPhase.SCANNING -> Unit
                    }
                }
            }
        }
    }

    manualFor?.let { student ->
        val question = state.current
        if (question != null) {
            ManualAnswerDialog(
                student = student,
                question = question,
                current = state.currentAnswers[student.studentId],
                onDismiss = { manualFor = null },
                onSelect = { answer ->
                    viewModel.setAnswer(student.studentId, answer)
                    if (!student.present && answer != null) viewModel.setPresent(student.studentId, true)
                    manualFor = null
                },
            )
        }
    }

    if (showAttendance) {
        AttendanceSheet(
            students = state.students,
            onSetPresent = { student, present -> viewModel.setPresent(student.studentId, present) },
            onDismiss = { showAttendance = false },
        )
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
private fun SessionMenu(canFinish: Boolean, onFinish: () -> Unit, onDelete: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            if (canFinish) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.finish_evaluation)) },
                    leadingIcon = { Icon(Icons.Outlined.Flag, null) },
                    onClick = {
                        open = false
                        onFinish()
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_evaluation)) },
                leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                onClick = {
                    open = false
                    onDelete()
                },
            )
        }
    }
}

/** Numbered chips for jumping between questions, coloured by status. */
@Composable
private fun QuestionStrip(state: SessionUiState, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.currentIndex) {
        listState.animateScrollToItem(state.currentIndex.coerceAtLeast(0))
    }
    LazyRow(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        itemsIndexed(state.questions, key = { _, q -> q.id }) { index, question ->
            FilterChip(
                selected = index == state.currentIndex,
                onClick = { onSelect(index) },
                label = { Text("${index + 1}") },
                leadingIcon = { QuestionStatusDot(question.status) },
            )
        }
        item(key = "wrap-up") {
            FilterChip(
                selected = state.currentIndex == state.questions.size,
                onClick = { onSelect(state.questions.size) },
                label = { Text(stringResource(R.string.quick_question_short)) },
                leadingIcon = { Icon(Icons.Filled.Add, null) },
            )
        }
    }
}

/**
 * The phone shows the presenter view full screen, e.g. while mirrored to a TV, with a
 * small control bar for the teacher.
 */
@Composable
private fun PresenterMode(
    presenterState: PresenterState?,
    phase: SessionPhase,
    revealed: Boolean,
    onExit: () -> Unit,
    onScan: () -> Unit,
    onReveal: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        PresenterContent(presenterState)
        Surface(
            color = Color.Black.copy(alpha = 0.55f),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
        ) {
            Row(
                Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.exit_presenter), tint = Color.White)
                }
                when (phase) {
                    SessionPhase.QUESTION -> Button(onClick = onScan) {
                        Icon(Icons.Outlined.CameraAlt, null)
                        Text(stringResource(R.string.scan_answers), Modifier.padding(start = 8.dp))
                    }
                    SessionPhase.RESULTS -> {
                        OutlinedButton(onClick = { onReveal(!revealed) }) {
                            Text(
                                stringResource(if (revealed) R.string.hide_results else R.string.reveal_results),
                                color = Color.White,
                            )
                        }
                        Button(onClick = onNext) { Text(stringResource(R.string.next)) }
                    }
                    else -> Unit
                }
            }
        }
    }
}
