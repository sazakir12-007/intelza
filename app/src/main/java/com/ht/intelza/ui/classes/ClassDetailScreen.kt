package com.ht.intelza.ui.classes

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.R
import com.ht.intelza.data.db.StudentEntity
import com.ht.intelza.domain.CardNumbers
import com.ht.intelza.ui.common.CardNumberBadge
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.TextInputDialog
import com.ht.intelza.ui.common.appViewModel

@Composable
fun ClassDetailScreen(
    onBack: () -> Unit,
    onPrintCards: (Long) -> Unit,
    onStartEvaluation: ((Long) -> Unit)? = null,
    onClassReport: ((Long) -> Unit)? = null,
    onStudentReport: ((Long) -> Unit)? = null,
) {
    val viewModel = appViewModel { c, s -> ClassDetailViewModel(c.application, c.classes, s) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingImport by viewModel.pendingImport.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    var menuOpen by remember { mutableStateOf(false) }
    var renaming by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<StudentEditTarget?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::readImportFile)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    val schoolClass = state.schoolClass
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(schoolClass?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { onPrintCards(viewModel.classId) }) {
                        Icon(Icons.Outlined.Print, stringResource(R.string.print_cards))
                    }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, stringResource(R.string.more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.import_students)) },
                            leadingIcon = { Icon(Icons.Outlined.UploadFile, null) },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(IMPORT_MIME_TYPES)
                            },
                        )
                        if (onClassReport != null) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.class_report)) },
                                leadingIcon = { Icon(Icons.Outlined.Insights, null) },
                                onClick = {
                                    menuOpen = false
                                    onClassReport(viewModel.classId)
                                },
                            )
                        }
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.rename)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = {
                                menuOpen = false
                                renaming = true
                            },
                        )
                        val archived = schoolClass?.archived == true
                        DropdownMenuItem(
                            text = { Text(stringResource(if (archived) R.string.unarchive else R.string.archive)) },
                            leadingIcon = { Icon(if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive, null) },
                            onClick = {
                                menuOpen = false
                                viewModel.setArchived(!archived)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_class)) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { editing = StudentEditTarget(null, "", "", viewModel.nextFreeCard()) },
                icon = { Icon(Icons.Filled.PersonAdd, contentDescription = null) },
                text = { Text(stringResource(R.string.add_student)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!state.loaded) return@Scaffold
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        pluralStringResource(R.plurals.student_count, state.students.size, state.students.size),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    if (onStartEvaluation != null && state.students.isNotEmpty() && schoolClass?.archived == false) {
                        AssistChip(
                            onClick = { onStartEvaluation(viewModel.classId) },
                            label = { Text(stringResource(R.string.start_evaluation)) },
                            leadingIcon = { Icon(Icons.Outlined.PlayArrow, null) },
                        )
                    }
                }
            }
            if (state.students.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.PersonOutline,
                        title = stringResource(R.string.no_students_title),
                        message = stringResource(R.string.no_students_message),
                        action = {
                            TextButton(onClick = { importLauncher.launch(IMPORT_MIME_TYPES) }) {
                                Text(stringResource(R.string.import_students))
                            }
                        },
                    )
                }
            }
            items(state.students, key = { it.id }) { student ->
                ListItem(
                    modifier = Modifier.clickable {
                        editing = StudentEditTarget(student.id, student.name, student.rollNumber, student.cardNumber)
                    },
                    headlineContent = { Text(student.name) },
                    supportingContent = {
                        if (student.rollNumber.isNotBlank()) {
                            Text(stringResource(R.string.roll_number_value, student.rollNumber))
                        }
                    },
                    leadingContent = { CardNumberBadge(student.cardNumber) },
                    trailingContent = if (onStudentReport != null) {
                        {
                            IconButton(onClick = { onStudentReport(student.id) }) {
                                Icon(Icons.Outlined.Insights, stringResource(R.string.student_report))
                            }
                        }
                    } else {
                        null
                    },
                )
                HorizontalDivider()
            }
        }
    }

    editing?.let { target ->
        StudentDialog(
            target = target,
            onDismiss = { editing = null },
            onSave = { name, roll, card, showError ->
                viewModel.saveStudent(target.studentId, name, roll, card) { error ->
                    if (error == null) editing = null else showError(error)
                }
            },
            onRemove = target.studentId?.let { id ->
                {
                    viewModel.removeStudent(id)
                    editing = null
                }
            },
        )
    }

    if (renaming && schoolClass != null) {
        TextInputDialog(
            title = stringResource(R.string.rename_class),
            label = stringResource(R.string.class_name_hint),
            initialValue = schoolClass.name,
            onDismiss = { renaming = false },
            onConfirm = {
                renaming = false
                viewModel.rename(it)
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_class_title),
            message = stringResource(R.string.delete_class_message),
            confirmLabel = stringResource(R.string.delete),
            onDismiss = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onBack)
            },
        )
    }

    pendingImport?.let { rows ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(pluralStringResource(R.plurals.import_found, rows.size, rows.size)) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 320.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        stringResource(R.string.import_explanation),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    for (row in rows) {
                        Text(
                            listOfNotNull(
                                row.name,
                                row.rollNumber.takeIf { it.isNotBlank() }?.let { stringResource(R.string.roll_number_value, it) },
                                row.cardNumber?.let { stringResource(R.string.card_number_value, it) },
                            ).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) { Text(stringResource(R.string.import_action)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

private data class StudentEditTarget(
    val studentId: Long?,
    val name: String,
    val rollNumber: String,
    val cardNumber: Int?,
)

@Composable
private fun StudentDialog(
    target: StudentEditTarget,
    onDismiss: () -> Unit,
    onSave: (name: String, roll: String, card: Int?, showError: (StudentFormError) -> Unit) -> Unit,
    onRemove: (() -> Unit)?,
) {
    var name by rememberSaveable { mutableStateOf(target.name) }
    var roll by rememberSaveable { mutableStateOf(target.rollNumber) }
    var card by rememberSaveable { mutableStateOf(target.cardNumber?.toString().orEmpty()) }
    var error by remember { mutableStateOf<StudentFormError?>(null) }
    var confirmRemove by remember { mutableStateOf(false) }

    val cardValue = card.toIntOrNull()
    val cardValid = cardValue != null && CardNumbers.isValid(cardValue)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (target.studentId == null) R.string.add_student else R.string.edit_student))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.student_name)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = roll,
                    onValueChange = { roll = it },
                    label = { Text(stringResource(R.string.roll_number_optional)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = card,
                    onValueChange = { value ->
                        card = value.filter(Char::isDigit).take(3)
                        error = null
                    },
                    label = { Text(stringResource(R.string.card_number)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = error != null || (card.isNotEmpty() && !cardValid),
                    supportingText = {
                        val text = when (val e = error) {
                            is StudentFormError.CardTaken -> stringResource(R.string.card_taken, e.takenBy)
                            StudentFormError.InvalidCard -> stringResource(R.string.card_invalid, CardNumbers.MAX)
                            StudentFormError.NoFreeCard -> stringResource(R.string.no_free_card)
                            null -> if (card.isNotEmpty() && !cardValid) {
                                stringResource(R.string.card_invalid, CardNumbers.MAX)
                            } else {
                                null
                            }
                        }
                        text?.let { Text(it) }
                    },
                )
                if (onRemove != null) {
                    TextButton(onClick = { confirmRemove = true }) {
                        Text(stringResource(R.string.remove_from_class), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, roll, cardValue) { error = it } },
                enabled = name.isNotBlank() && cardValid,
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )

    if (confirmRemove && onRemove != null) {
        ConfirmDialog(
            title = stringResource(R.string.remove_student_title, target.name),
            message = stringResource(R.string.remove_student_message),
            confirmLabel = stringResource(R.string.remove),
            onDismiss = { confirmRemove = false },
            onConfirm = {
                confirmRemove = false
                onRemove()
            },
        )
    }
}

private val IMPORT_MIME_TYPES = arrayOf(
    "text/csv",
    "text/comma-separated-values",
    "text/plain",
    "text/tab-separated-values",
    "application/csv",
    "application/vnd.ms-excel",
)
