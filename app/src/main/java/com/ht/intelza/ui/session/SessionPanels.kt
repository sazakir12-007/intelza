package com.ht.intelza.ui.session

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ht.intelza.R
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.data.db.SessionStudentRow
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionStatus
import com.ht.intelza.domain.QuestionType
import com.ht.intelza.ui.common.AnswerBars
import com.ht.intelza.ui.common.CardNumberBadge
import com.ht.intelza.ui.common.LetterTile
import com.ht.intelza.ui.common.formatPercent
import com.ht.intelza.ui.questions.StoredImage
import com.ht.intelza.ui.questions.optionText
import com.ht.intelza.ui.questions.questionTypeLabel
import com.ht.intelza.ui.theme.AnswerColors
import com.ht.intelza.ui.theme.ResultColors

fun SessionQuestionEntity.optionTextFor(option: AnswerOption): String = when (option) {
    AnswerOption.A -> optionA
    AnswerOption.B -> optionB
    AnswerOption.C -> optionC
    AnswerOption.D -> optionD
}

val SessionQuestionEntity.options: List<AnswerOption>
    get() = AnswerOption.firstN(type.optionCount(optionCount))

/** The question as the teacher sees it, with the correct answer marked. */
@Composable
fun QuestionCard(question: SessionQuestionEntity, modifier: Modifier = Modifier) {
    ElevatedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                questionTypeLabel(question.type),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            if (question.text.isNotBlank()) {
                Text(question.text, style = MaterialTheme.typography.titleLarge)
            }
            question.imageName?.let {
                StoredImage(
                    name = it,
                    contentDescription = stringResource(R.string.question_picture),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
            for (option in question.options) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LetterTile(option.name, AnswerColors.of(option))
                    Text(
                        optionText(question.type, option, question.optionTextFor(option)),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (question.correct == option) {
                        Icon(Icons.Filled.CheckCircle, stringResource(R.string.correct_answer), tint = ResultColors.correct)
                    }
                }
            }
        }
    }
}

@Composable
fun QuestionPanel(
    question: SessionQuestionEntity,
    onScan: () -> Unit,
    onSkip: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuestionCard(question)
        if (question.status == QuestionStatus.SKIPPED) {
            Text(
                stringResource(R.string.question_was_skipped),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.CameraAlt, null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.scan_answers))
        }
        if (question.status != QuestionStatus.SKIPPED) {
            OutlinedButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.skip_question))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResultsPanel(
    state: SessionUiState,
    presenting: Boolean,
    onScanAgain: () -> Unit,
    onSetCorrect: (AnswerOption?) -> Unit,
    onStudentTapped: (SessionStudentRow) -> Unit,
    onReveal: (Boolean) -> Unit,
    onNext: () -> Unit,
) {
    val question = state.current ?: return
    val result = state.currentResult ?: return
    val isLast = state.currentIndex == state.questions.lastIndex
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        QuestionCard(question)

        if (question.type != QuestionType.POLL && question.correct == null) {
            ElevatedCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.which_answer_correct), style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (option in question.options) {
                            AssistChip(
                                onClick = { onSetCorrect(option) },
                                label = { Text(optionText(question.type, option, option.name)) },
                                leadingIcon = { LetterTile(option.name, AnswerColors.of(option), size = 24.dp) },
                            )
                        }
                        AssistChip(
                            onClick = { onSetCorrect(null) },
                            label = { Text(stringResource(R.string.no_correct_answer_poll)) },
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                result.fractionCorrect?.let {
                    Text(
                        formatPercent(it),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (it * 100 < state.settings.reteachThreshold) ResultColors.incorrect else ResultColors.correct,
                    )
                    Text(stringResource(R.string.got_it_right), style = MaterialTheme.typography.bodyMedium)
                } ?: Text(stringResource(R.string.poll_results), style = MaterialTheme.typography.titleLarge)
            }
            Text(
                stringResource(R.string.answered_count, result.answered, result.present),
                style = MaterialTheme.typography.titleMedium,
            )
        }

        AnswerBars(
            options = question.options,
            counts = result.counts,
            total = result.present,
            correct = question.correct,
            noAnswerCount = result.noAnswer.size,
        )

        Text(stringResource(R.string.who_chose_what), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.tap_to_change_answer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        for (option in question.options) {
            val students = result.studentsByAnswer[option].orEmpty()
            if (students.isEmpty()) continue
            StudentGroup(
                header = {
                    LetterTile(option.name, AnswerColors.of(option), size = 24.dp)
                    Text(
                        optionText(question.type, option, question.optionTextFor(option)).ifBlank { option.name },
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                students = students,
                onStudentTapped = onStudentTapped,
            )
        }
        if (result.noAnswer.isNotEmpty()) {
            StudentGroup(
                header = { Text(stringResource(R.string.no_answer), style = MaterialTheme.typography.labelLarge) },
                students = result.noAnswer,
                onStudentTapped = onStudentTapped,
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onScanAgain, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.scan_again))
            }
            if (presenting) {
                OutlinedButton(onClick = { onReveal(!state.revealed) }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (state.revealed) R.string.hide_results else R.string.reveal_results))
                }
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (isLast) R.string.continue_label else R.string.next_question))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StudentGroup(
    header: @Composable () -> Unit,
    students: List<SessionStudentRow>,
    onStudentTapped: (SessionStudentRow) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            header()
            Text("(${students.size})", style = MaterialTheme.typography.labelLarge)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (student in students) {
                AssistChip(onClick = { onStudentTapped(student) }, label = { Text(student.name) })
            }
        }
    }
}

/** Shown after the last question: ask a quick question or finish (requirement D7). */
@Composable
fun WrapUpPanel(
    state: SessionUiState,
    onAskQuick: (QuestionType, Int, String) -> Unit,
    onFinish: () -> Unit,
) {
    var type by rememberSaveable { mutableStateOf(QuestionType.MULTIPLE_CHOICE) }
    var optionCount by rememberSaveable { mutableIntStateOf(4) }
    var text by rememberSaveable { mutableStateOf("") }
    val asked = state.questions.count { it.status == QuestionStatus.ASKED }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (!state.isQuickSession && state.questions.isNotEmpty()) {
            Text(stringResource(R.string.end_of_prepared_questions), style = MaterialTheme.typography.titleMedium)
        }
        ElevatedCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.ask_quick_question), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.ask_quick_question_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    QuestionType.entries.forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = type == value,
                            onClick = { type = value },
                            shape = SegmentedButtonDefaults.itemShape(index, QuestionType.entries.size),
                        ) { Text(questionTypeLabel(value), maxLines = 1) }
                    }
                }
                if (type != QuestionType.TRUE_FALSE) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        (2..4).forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = optionCount == count,
                                onClick = { optionCount = count },
                                shape = SegmentedButtonDefaults.itemShape(index, 3),
                            ) { Text(pluralStringResource(R.plurals.options_short, count, count)) }
                        }
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.question_text_optional)) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = {
                        onAskQuick(type, optionCount, text)
                        text = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.CameraAlt, null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.scan_answers))
                }
            }
        }
        if (asked > 0) {
            OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Flag, null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.finish_evaluation))
            }
        }
    }
}

/** Lets the teacher set or clear one student's answer by hand (requirement D5). */
@Composable
fun ManualAnswerDialog(
    student: SessionStudentRow,
    question: SessionQuestionEntity,
    current: AnswerOption?,
    onDismiss: () -> Unit,
    onSelect: (AnswerOption?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(student.name) },
        text = {
            Column {
                for (option in question.options) {
                    AnswerChoiceRow(
                        selected = current == option,
                        onClick = { onSelect(option) },
                        label = {
                            LetterTile(option.name, AnswerColors.of(option), size = 28.dp)
                            Text(optionText(question.type, option, question.optionTextFor(option)))
                        },
                    )
                }
                AnswerChoiceRow(
                    selected = current == null,
                    onClick = { onSelect(null) },
                    label = { Text(stringResource(R.string.no_answer)) },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun AnswerChoiceRow(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        label()
    }
}

/** Marks students absent or present for this session (requirement A4). */
@Composable
fun AttendanceSheet(
    students: List<SessionStudentRow>,
    onSetPresent: (SessionStudentRow, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            stringResource(R.string.attendance),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Text(
            stringResource(R.string.attendance_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
        )
        LazyColumn(Modifier.padding(bottom = 24.dp)) {
            items(students, key = { it.studentId }) { student ->
                ListItem(
                    modifier = Modifier.clickable { onSetPresent(student, !student.present) },
                    leadingContent = { CardNumberBadge(student.cardNumber, size = 36.dp) },
                    headlineContent = { Text(student.name) },
                    supportingContent = {
                        Text(stringResource(if (student.present) R.string.present else R.string.absent))
                    },
                    trailingContent = {
                        Switch(checked = student.present, onCheckedChange = { onSetPresent(student, it) })
                    },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun QuestionStatusDot(status: QuestionStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        QuestionStatus.ASKED -> ResultColors.correct
        QuestionStatus.SKIPPED -> ResultColors.warning
        QuestionStatus.PENDING -> MaterialTheme.colorScheme.outline
    }
    androidx.compose.foundation.Canvas(modifier.size(8.dp)) { drawCircle(color) }
}
