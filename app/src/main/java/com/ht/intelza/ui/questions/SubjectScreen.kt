package com.ht.intelza.ui.questions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.db.SubjectEntity
import com.ht.intelza.data.db.TopicSummary
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.TextInputDialog
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.CompactListItem

class SubjectViewModel(
    private val questions: QuestionRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val subjectId: Long = checkNotNull(savedState["subjectId"])

    val subject: StateFlow<SubjectEntity?> = questions.observeSubject(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val topics: StateFlow<List<TopicSummary>?> = questions.observeTopics(subjectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addTopic(name: String, grade: String, onCreated: (Long) -> Unit) = viewModelScope.launch {
        onCreated(questions.addTopic(subjectId, name, grade))
    }

    fun rename(name: String) = viewModelScope.launch { questions.renameSubject(subjectId, name) }

    fun delete(onDeleted: () -> Unit) = viewModelScope.launch {
        questions.deleteSubject(subjectId)
        onDeleted()
    }
}

@Composable
fun SubjectScreen(onBack: () -> Unit, onOpenTopic: (Long) -> Unit) {
    val viewModel = appViewModel { c, s -> SubjectViewModel(c.questions, s) }
    val subject by viewModel.subject.collectAsStateWithLifecycle()
    val topics by viewModel.topics.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(subject?.name.orEmpty()) },
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
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuOpen = false
                                renaming = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuOpen = false
                                deleting = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_topic)) },
            )
        },
    ) { padding ->
        val list = topics ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Topic,
                        title = stringResource(R.string.no_topics_title),
                        message = stringResource(R.string.no_topics_message),
                    )
                }
            }
            items(list, key = { it.topic.id }) { summary ->
                val details = listOfNotNull(
                    summary.topic.grade.takeIf { it.isNotBlank() }?.let { stringResource(R.string.grade_value, it) },
                    pluralStringResource(R.plurals.question_count, summary.questionCount, summary.questionCount),
                    summary.lastEvaluatedAt?.let { stringResource(R.string.last_evaluated, formatDate(it)) },
                )
                CompactListItem(
                    modifier = Modifier.clickable { onOpenTopic(summary.topic.id) },
                    headlineContent = { Text(summary.topic.name) },
                    supportingContent = { Text(details.joinToString(" · ")) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                )
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        TopicDialog(
            title = stringResource(R.string.new_topic),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { adding = false },
            onConfirm = { name, grade ->
                adding = false
                viewModel.addTopic(name, grade, onOpenTopic)
            },
        )
    }
    if (renaming) {
        TextInputDialog(
            title = stringResource(R.string.rename_subject),
            label = stringResource(R.string.subject_name),
            initialValue = subject?.name.orEmpty(),
            onDismiss = { renaming = false },
            onConfirm = {
                renaming = false
                viewModel.rename(it)
            },
        )
    }
    if (deleting) {
        ConfirmDialog(
            title = stringResource(R.string.delete_subject_title, subject?.name.orEmpty()),
            message = stringResource(R.string.delete_subject_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deleting = false },
            onConfirm = {
                deleting = false
                viewModel.delete(onBack)
            },
        )
    }
}

/** Name and optional grade of a topic. */
@Composable
fun TopicDialog(
    title: String,
    confirmLabel: String,
    initialName: String = "",
    initialGrade: String = "",
    onDismiss: () -> Unit,
    onConfirm: (name: String, grade: String) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var grade by rememberSaveable { mutableStateOf(initialGrade) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.topic_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                )
                OutlinedTextField(
                    value = grade,
                    onValueChange = { grade = it },
                    label = { Text(stringResource(R.string.grade_optional)) },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name.trim(), grade.trim()) }, enabled = name.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
