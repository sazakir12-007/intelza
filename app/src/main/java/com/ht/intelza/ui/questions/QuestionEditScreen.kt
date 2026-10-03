package com.ht.intelza.ui.questions

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.R
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionType
import com.ht.intelza.ui.common.LetterTile
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.theme.AnswerColors
import kotlinx.coroutines.launch
import com.ht.intelza.ui.common.AppTopBar

@Composable
fun QuestionEditScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, s -> QuestionEditViewModel(c.questions, c.images, s) }
    val form by viewModel.form.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedMessage = stringResource(R.string.question_saved)
    var captureUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::importImage)
    }
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val uri = captureUri
        if (saved && uri != null) viewModel.importImage(uri)
    }
    val noCameraApp = stringResource(R.string.no_camera_app)
    fun launchCamera() {
        val uri = viewModel.newCaptureUri()
        captureUri = uri
        try {
            takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            scope.launch { snackbar.showSnackbar(noCameraApp) }
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(if (viewModel.isNew) R.string.new_question else R.string.edit_question)) },
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
                        .imePadding()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (viewModel.isNew) {
                        OutlinedButton(
                            onClick = {
                                viewModel.save(addAnother = true) {
                                    scope.launch { snackbar.showSnackbar(savedMessage) }
                                }
                            },
                            enabled = form.canSave,
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(R.string.save_and_add_another)) }
                    }
                    Button(
                        onClick = { viewModel.save(addAnother = false, onSaved = onBack) },
                        enabled = form.canSave,
                        modifier = Modifier.weight(1f),
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (!form.loaded) return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                QuestionType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = form.type == type,
                        onClick = { viewModel.setType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, QuestionType.entries.size),
                    ) { Text(questionTypeLabel(type), maxLines = 1) }
                }
            }
            Text(
                stringResource(
                    when (form.type) {
                        QuestionType.MULTIPLE_CHOICE -> R.string.type_multiple_choice_hint
                        QuestionType.TRUE_FALSE -> R.string.type_true_false_hint
                        QuestionType.POLL -> R.string.type_poll_hint
                    },
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = form.text,
                onValueChange = viewModel::setText,
                label = { Text(stringResource(R.string.question_text)) },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            PictureSection(
                imageName = form.imageName,
                importing = form.importingImage,
                failed = form.imageError,
                onPick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                onTakePhoto = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
                },
                onRemove = viewModel::removeImage,
            )

            if (form.type != QuestionType.TRUE_FALSE) {
                Text(stringResource(R.string.number_of_options), style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    (2..4).forEachIndexed { index, count ->
                        SegmentedButton(
                            selected = form.optionCount == count,
                            onClick = { viewModel.setOptionCount(count) },
                            shape = SegmentedButtonDefaults.itemShape(index, 3),
                        ) { Text(count.toString()) }
                    }
                }
            }

            Text(
                stringResource(
                    if (form.type == QuestionType.POLL) R.string.answer_options else R.string.answer_options_mark_correct,
                ),
                style = MaterialTheme.typography.titleSmall,
            )
            for (option in form.visibleOptions) {
                OptionRow(
                    option = option,
                    type = form.type,
                    text = form.options[option.ordinal],
                    isCorrect = form.correct == option,
                    onText = { viewModel.setOption(option, it) },
                    onMarkCorrect = { viewModel.setCorrect(option) },
                )
            }
            if (form.type != QuestionType.POLL && form.correct == null) {
                Text(
                    stringResource(R.string.choose_correct_answer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun PictureSection(
    imageName: String?,
    importing: Boolean,
    failed: Boolean,
    onPick: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.picture_optional), style = MaterialTheme.typography.titleSmall)
        when {
            importing -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.padding(24.dp))
            }
            imageName != null -> {
                StoredImage(
                    name = imageName,
                    contentDescription = stringResource(R.string.question_picture),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onPick) { Text(stringResource(R.string.replace_picture)) }
                    TextButton(onClick = onRemove) {
                        Icon(Icons.Outlined.Delete, null)
                        Text(stringResource(R.string.remove), Modifier.padding(start = 4.dp))
                    }
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPick) {
                    Icon(Icons.Outlined.AddPhotoAlternate, null)
                    Text(stringResource(R.string.choose_picture), Modifier.padding(start = 8.dp))
                }
                OutlinedButton(onClick = onTakePhoto) {
                    Icon(Icons.Outlined.PhotoCamera, null)
                    Text(stringResource(R.string.take_photo), Modifier.padding(start = 8.dp))
                }
            }
        }
        if (failed) {
            Text(
                stringResource(R.string.picture_failed),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OptionRow(
    option: AnswerOption,
    type: QuestionType,
    text: String,
    isCorrect: Boolean,
    onText: (String) -> Unit,
    onMarkCorrect: () -> Unit,
) {
    val scored = type != QuestionType.POLL
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = if (type == QuestionType.TRUE_FALSE) {
            Modifier.selectable(selected = isCorrect, onClick = onMarkCorrect, role = Role.RadioButton)
        } else {
            Modifier
        },
    ) {
        LetterTile(option.name, AnswerColors.of(option))
        if (type == QuestionType.TRUE_FALSE) {
            Text(
                optionText(type, option, ""),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
        } else {
            OutlinedTextField(
                value = text,
                onValueChange = onText,
                label = { Text(stringResource(R.string.option_label_optional, option.name)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
        if (scored) {
            RadioButton(selected = isCorrect, onClick = onMarkCorrect)
        }
    }
}
