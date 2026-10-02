package com.ht.intelza.ui.questions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.db.QuestionEntity
import com.ht.intelza.data.db.TopicWithSubject
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TopicViewModel(
    private val questions: QuestionRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val topicId: Long = checkNotNull(savedState["topicId"])

    val topic: StateFlow<TopicWithSubject?> = questions.observeTopic(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val questionList: StateFlow<List<QuestionEntity>?> = questions.observeQuestions(topicId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun updateTopic(name: String, grade: String) = viewModelScope.launch { questions.updateTopic(topicId, name, grade) }

    fun deleteTopic(onDeleted: () -> Unit) = viewModelScope.launch {
        questions.deleteTopic(topicId)
        onDeleted()
    }

    fun duplicate(id: Long) = viewModelScope.launch { questions.duplicateQuestion(id) }

    fun move(from: Int, to: Int) = viewModelScope.launch { questions.moveQuestion(topicId, from, to) }

    fun delete(id: Long) = viewModelScope.launch { questions.deleteQuestion(id) }
}

@Composable
fun TopicScreen(
    onBack: () -> Unit,
    onAddQuestion: (topicId: Long) -> Unit,
    onEditQuestion: (topicId: Long, questionId: Long) -> Unit,
    onStartEvaluation: ((topicId: Long) -> Unit)? = null,
    onTopicReport: ((topicId: Long) -> Unit)? = null,
) {
    val viewModel = appViewModel { c, s -> TopicViewModel(c.questions, s) }
    val topic by viewModel.topic.collectAsStateWithLifecycle()
    val questions by viewModel.questionList.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var editingTopic by rememberSaveable { mutableStateOf(false) }
    var deletingTopic by rememberSaveable { mutableStateOf(false) }
    var deletingQuestion by remember { mutableStateOf<QuestionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(topic?.topic?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (onTopicReport != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.topic_report)) },
                                leadingIcon = { Icon(Icons.Outlined.Insights, null) },
                                onClick = {
                                    menuOpen = false
                                    onTopicReport(viewModel.topicId)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.edit_topic)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuOpen = false
                                editingTopic = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_topic)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuOpen = false
                                deletingTopic = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddQuestion(viewModel.topicId) },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.add_question)) },
            )
        },
    ) { padding ->
        val list = questions ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 96.dp)) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val info = listOfNotNull(
                        topic?.subjectName,
                        topic?.topic?.grade?.takeIf { it.isNotBlank() }?.let { stringResource(R.string.grade_value, it) },
                        pluralStringResource(R.plurals.question_count, list.size, list.size),
                    )
                    Text(
                        info.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    if (onStartEvaluation != null && list.isNotEmpty()) {
                        AssistChip(
                            onClick = { onStartEvaluation(viewModel.topicId) },
                            label = { Text(stringResource(R.string.evaluate)) },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) },
                        )
                    }
                }
            }
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Quiz,
                        title = stringResource(R.string.no_questions_title),
                        message = stringResource(R.string.no_questions_message),
                    )
                }
            }
            itemsIndexed(list, key = { _, q -> q.id }) { index, question ->
                QuestionRow(
                    index = index,
                    question = question,
                    isFirst = index == 0,
                    isLast = index == list.lastIndex,
                    onClick = { onEditQuestion(viewModel.topicId, question.id) },
                    onDuplicate = { viewModel.duplicate(question.id) },
                    onMoveUp = { viewModel.move(index, index - 1) },
                    onMoveDown = { viewModel.move(index, index + 1) },
                    onDelete = { deletingQuestion = question },
                )
                HorizontalDivider()
            }
        }
    }

    if (editingTopic) {
        TopicDialog(
            title = stringResource(R.string.edit_topic),
            confirmLabel = stringResource(R.string.save),
            initialName = topic?.topic?.name.orEmpty(),
            initialGrade = topic?.topic?.grade.orEmpty(),
            onDismiss = { editingTopic = false },
            onConfirm = { name, grade ->
                editingTopic = false
                viewModel.updateTopic(name, grade)
            },
        )
    }
    if (deletingTopic) {
        ConfirmDialog(
            title = stringResource(R.string.delete_topic_title, topic?.topic?.name.orEmpty()),
            message = stringResource(R.string.delete_topic_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deletingTopic = false },
            onConfirm = {
                deletingTopic = false
                viewModel.deleteTopic(onBack)
            },
        )
    }
    deletingQuestion?.let { question ->
        ConfirmDialog(
            title = stringResource(R.string.delete_question_title),
            message = stringResource(R.string.delete_question_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deletingQuestion = null },
            onConfirm = {
                deletingQuestion = null
                viewModel.delete(question.id)
            },
        )
    }
}

@Composable
private fun QuestionRow(
    index: Int,
    question: QuestionEntity,
    isFirst: Boolean,
    isLast: Boolean,
    onClick: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${index + 1}", style = MaterialTheme.typography.titleSmall)
                }
            }
        },
        headlineContent = {
            if (question.text.isBlank()) {
                Text(stringResource(R.string.picture_question), fontStyle = FontStyle.Italic)
            } else {
                Text(question.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        },
        supportingContent = {
            Text(questionSummary(question.type, question.optionCount, question.correct, question.optionA))
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (question.imageName != null) {
                    Icon(
                        Icons.Outlined.Image,
                        stringResource(R.string.has_picture),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicate)) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null) },
                            onClick = {
                                menuOpen = false
                                onDuplicate()
                            },
                        )
                        if (!isFirst) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_up)) },
                                leadingIcon = { Icon(Icons.Outlined.ArrowUpward, null) },
                                onClick = {
                                    menuOpen = false
                                    onMoveUp()
                                },
                            )
                        }
                        if (!isLast) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.move_down)) },
                                leadingIcon = { Icon(Icons.Outlined.ArrowDownward, null) },
                                onClick = {
                                    menuOpen = false
                                    onMoveDown()
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
    )
}
