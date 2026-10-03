package com.ht.intelza.ui.classes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.clickable
import com.ht.intelza.R
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.db.ClassSummary
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.TextInputDialog
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.DrawerMenuButton
import com.ht.intelza.ui.common.CompactListItem

class ClassListViewModel(private val classes: ClassRepository) : ViewModel() {
    val summaries: StateFlow<List<ClassSummary>?> = classes.observeSummaries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun createClass(name: String, onCreated: (Long) -> Unit) {
        viewModelScope.launch { onCreated(classes.createClass(name)) }
    }
}

@Composable
fun ClassListScreen(
    onOpenClass: (Long) -> Unit,
    onPrintNumberedCards: () -> Unit,
    onSettings: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> ClassListViewModel(c.classes) }
    val summaries by viewModel.summaries.collectAsStateWithLifecycle()
    var showCreate by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.nav_classes)) },
                navigationIcon = { DrawerMenuButton() },
                actions = {
                    IconButton(onClick = onPrintNumberedCards) {
                        Icon(Icons.Outlined.Print, stringResource(R.string.print_cards))
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreate = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.new_class)) },
            )
        },
    ) { padding ->
        val list = summaries ?: return@Scaffold
        val active = list.filterNot { it.schoolClass.archived }
        val archived = list.filter { it.schoolClass.archived }
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            if (active.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Groups,
                        title = stringResource(R.string.no_classes_title),
                        message = stringResource(R.string.no_classes_message),
                    )
                }
            }
            items(active, key = { it.schoolClass.id }) { summary ->
                ClassRow(summary, onClick = { onOpenClass(summary.schoolClass.id) })
                HorizontalDivider()
            }
            if (archived.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.archived)) }
                items(archived, key = { it.schoolClass.id }) { summary ->
                    ClassRow(summary, onClick = { onOpenClass(summary.schoolClass.id) }, archived = true)
                    HorizontalDivider()
                }
            }
        }
    }

    if (showCreate) {
        TextInputDialog(
            title = stringResource(R.string.new_class),
            label = stringResource(R.string.class_name_hint),
            confirmLabel = stringResource(R.string.create),
            onDismiss = { showCreate = false },
            onConfirm = { name ->
                showCreate = false
                viewModel.createClass(name, onOpenClass)
            },
        )
    }
}

@Composable
private fun ClassRow(summary: ClassSummary, onClick: () -> Unit, archived: Boolean = false) {
    val students = pluralStringResource(R.plurals.student_count, summary.studentCount, summary.studentCount)
    val last = summary.lastSessionAt?.let { stringResource(R.string.last_evaluated, formatDate(it)) }
    CompactListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(summary.schoolClass.name) },
        supportingContent = { Text(listOfNotNull(students, last).joinToString(" · ")) },
        leadingContent = {
            Icon(if (archived) Icons.Outlined.Inventory2 else Icons.Outlined.Groups, contentDescription = null)
        },
        trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null) },
    )
}
