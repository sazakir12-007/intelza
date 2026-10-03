package com.ht.intelza.ui.questions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.db.SubjectSummary
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.TextInputDialog
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.DrawerMenuButton
import com.ht.intelza.ui.common.CompactListItem

class SubjectsViewModel(private val questions: QuestionRepository) : ViewModel() {
    val subjects: StateFlow<List<SubjectSummary>?> = questions.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun add(name: String) = viewModelScope.launch { questions.addSubject(name) }

    fun rename(id: Long, name: String) = viewModelScope.launch { questions.renameSubject(id, name) }

    fun delete(id: Long) = viewModelScope.launch { questions.deleteSubject(id) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SubjectsScreen(
    onOpenSubject: (Long) -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> SubjectsViewModel(c.questions) }
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<SubjectSummary?>(null) }
    var renaming by remember { mutableStateOf<SubjectSummary?>(null) }
    var deleting by remember { mutableStateOf<SubjectSummary?>(null) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.question_bank)) },
                navigationIcon = { DrawerMenuButton() },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { adding = true },
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_subject)) },
            )
        },
    ) { padding ->
        val list = subjects ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 88.dp)) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        title = stringResource(R.string.no_subjects_title),
                        message = stringResource(R.string.no_subjects_message),
                    )
                }
            }
            items(list, key = { it.subject.id }) { summary ->
                Box {
                    CompactListItem(
                        modifier = Modifier.combinedClickable(
                            onClick = { onOpenSubject(summary.subject.id) },
                            onLongClick = { menuFor = summary },
                        ),
                        headlineContent = { Text(summary.subject.name) },
                        supportingContent = {
                            Text(
                                pluralStringResource(R.plurals.topic_count, summary.topicCount, summary.topicCount) +
                                    " Â· " +
                                    pluralStringResource(R.plurals.question_count, summary.questionCount, summary.questionCount),
                            )
                        },
                        leadingContent = { Icon(Icons.AutoMirrored.Outlined.MenuBook, null) },
                        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null) },
                    )
                    DropdownMenu(expanded = menuFor == summary, onDismissRequest = { menuFor = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuFor = null
                                renaming = summary
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuFor = null
                                deleting = summary
                            },
                        )
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (adding) {
        TextInputDialog(
            title = stringResource(R.string.new_subject),
            label = stringResource(R.string.subject_name),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { adding = false },
            onConfirm = {
                adding = false
                viewModel.add(it)
            },
        )
    }
    renaming?.let { summary ->
        TextInputDialog(
            title = stringResource(R.string.rename_subject),
            label = stringResource(R.string.subject_name),
            initialValue = summary.subject.name,
            onDismiss = { renaming = null },
            onConfirm = {
                renaming = null
                viewModel.rename(summary.subject.id, it)
            },
        )
    }
    deleting?.let { summary ->
        ConfirmDialog(
            title = stringResource(R.string.delete_subject_title, summary.subject.name),
            message = stringResource(R.string.delete_subject_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { deleting = null },
            onConfirm = {
                deleting = null
                viewModel.delete(summary.subject.id)
            },
        )
    }
}
