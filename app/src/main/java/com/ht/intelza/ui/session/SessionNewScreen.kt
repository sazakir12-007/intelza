package com.ht.intelza.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.SessionRepository
import com.ht.intelza.data.db.ClassSummary
import com.ht.intelza.data.db.TopicWithSubject
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import androidx.compose.material.icons.outlined.Groups
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.CompactListItem

/** What the session will ask: a topic from the bank, or quick questions made up on the spot. */
sealed interface SessionContent {
    data class Topic(val topicId: Long) : SessionContent
    data object Quick : SessionContent
}

class SessionNewViewModel(
    classes: ClassRepository,
    questions: QuestionRepository,
    private val sessions: SessionRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val classList: StateFlow<List<ClassSummary>?> = classes.observeSummaries()
        .map { list -> list.filter { !it.schoolClass.archived && it.studentCount > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topics: StateFlow<List<TopicWithSubject>?> = questions.observeAllTopics()
        .map { list -> list.filter { it.questionCount > 0 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _selectedClass = MutableStateFlow(savedState.get<Long>("classId")?.takeIf { it > 0 })
    val selectedClass: StateFlow<Long?> = _selectedClass.asStateFlow()

    private val _content = MutableStateFlow<SessionContent?>(
        savedState.get<Long>("topicId")?.takeIf { it > 0 }?.let { SessionContent.Topic(it) },
    )
    val content: StateFlow<SessionContent?> = _content.asStateFlow()

    private var starting = false

    fun selectClass(id: Long) {
        _selectedClass.value = id
    }

    fun selectContent(content: SessionContent) {
        _content.value = content
    }

    fun start(onStarted: (Long) -> Unit) {
        val classId = _selectedClass.value ?: return
        val content = _content.value ?: return
        if (starting) return
        starting = true
        viewModelScope.launch {
            val topicId = (content as? SessionContent.Topic)?.topicId
            onStarted(sessions.startSession(classId, topicId))
        }
    }
}

@Composable
fun SessionNewScreen(onBack: () -> Unit, onStarted: (sessionId: Long) -> Unit) {
    val viewModel = appViewModel { c, s -> SessionNewViewModel(c.classes, c.questions, c.sessions, s) }
    val classes by viewModel.classList.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val content by viewModel.content.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.start_evaluation)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { viewModel.start(onStarted) },
                        enabled = selectedClass != null && content != null,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.start)) }
                }
            }
        },
    ) { padding ->
        val classList = classes ?: return@Scaffold
        val topicList = topics ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item { SectionHeader(stringResource(R.string.step_choose_class)) }
            if (classList.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Groups,
                        title = stringResource(R.string.no_ready_classes_title),
                        message = stringResource(R.string.no_ready_classes_message),
                    )
                }
            }
            items(classList, key = { "class-${it.schoolClass.id}" }) { summary ->
                SelectableRow(
                    selected = selectedClass == summary.schoolClass.id,
                    onSelect = { viewModel.selectClass(summary.schoolClass.id) },
                    title = summary.schoolClass.name,
                    subtitle = pluralStringResource(R.plurals.student_count, summary.studentCount, summary.studentCount),
                )
            }

            item { SectionHeader(stringResource(R.string.step_choose_questions)) }
            item {
                SelectableRow(
                    selected = content == SessionContent.Quick,
                    onSelect = { viewModel.selectContent(SessionContent.Quick) },
                    title = stringResource(R.string.quick_session),
                    subtitle = stringResource(R.string.quick_session_hint),
                    leading = { Icon(Icons.Outlined.Bolt, null, tint = MaterialTheme.colorScheme.tertiary) },
                )
            }
            items(topicList, key = { "topic-${it.topic.id}" }) { topic ->
                SelectableRow(
                    selected = (content as? SessionContent.Topic)?.topicId == topic.topic.id,
                    onSelect = { viewModel.selectContent(SessionContent.Topic(topic.topic.id)) },
                    title = topic.topic.name,
                    subtitle = listOfNotNull(
                        topic.subjectName,
                        topic.topic.grade.takeIf { it.isNotBlank() }?.let { stringResource(R.string.grade_value, it) },
                        pluralStringResource(R.plurals.question_count, topic.questionCount, topic.questionCount),
                    ).joinToString(" · "),
                )
            }
            if (topicList.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.no_topics_for_session),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectableRow(
    selected: Boolean,
    onSelect: () -> Unit,
    title: String,
    subtitle: String,
    leading: (@Composable () -> Unit)? = null,
) {
    CompactListItem(
        modifier = Modifier.selectable(selected = selected, onClick = onSelect, role = Role.RadioButton),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = leading,
        trailingContent = { RadioButton(selected = selected, onClick = null) },
        containerColor = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        } else {
            Color.Transparent
        },
    )
}
