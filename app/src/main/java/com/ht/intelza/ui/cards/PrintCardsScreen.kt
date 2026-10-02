package com.ht.intelza.ui.cards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ht.intelza.R
import com.ht.intelza.data.CardSize
import com.ht.intelza.data.PaperSize
import com.ht.intelza.domain.CardNumbers
import com.ht.intelza.ui.common.appViewModel

@Composable
fun PrintCardsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { c, s -> PrintCardsViewModel(c.application, c.classes, c.settings, s) }
    val options by viewModel.options.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val className by viewModel.className.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.print_cards)) },
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
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = viewModel::share,
                        enabled = preview != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text(stringResource(R.string.share_pdf), Modifier.padding(start = 8.dp))
                    }
                    Button(
                        onClick = viewModel::print,
                        enabled = preview != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.Print, contentDescription = null)
                        Text(stringResource(R.string.print), Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
    ) { padding ->
        val current = options ?: return@Scaffold
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (className != null) {
                OptionLabel(stringResource(R.string.cards_for))
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = current.forClass,
                        onClick = { viewModel.update { it.copy(forClass = true) } },
                        shape = SegmentedButtonDefaults.itemShape(0, 2),
                    ) { Text(className.orEmpty()) }
                    SegmentedButton(
                        selected = !current.forClass,
                        onClick = { viewModel.update { it.copy(forClass = false) } },
                        shape = SegmentedButtonDefaults.itemShape(1, 2),
                    ) { Text(stringResource(R.string.numbered_set)) }
                }
            }

            if (current.forClass) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.print_student_names), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.print_student_names_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = current.includeNames,
                        onCheckedChange = { checked -> viewModel.update { it.copy(includeNames = checked) } },
                    )
                }
            } else {
                Text(
                    stringResource(R.string.numbered_set_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(
                        label = stringResource(R.string.from_card),
                        value = current.rangeFrom,
                        onValue = { v -> viewModel.update { it.copy(rangeFrom = v) } },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        label = stringResource(R.string.to_card),
                        value = current.rangeTo,
                        onValue = { v -> viewModel.update { it.copy(rangeTo = v) } },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            OptionLabel(stringResource(R.string.card_size))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                CardSize.entries.forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = current.size == size,
                        onClick = { viewModel.update { it.copy(size = size) } },
                        shape = SegmentedButtonDefaults.itemShape(index, CardSize.entries.size),
                    ) {
                        Text(stringResource(if (size == CardSize.STANDARD) R.string.card_size_standard else R.string.card_size_large))
                    }
                }
            }

            OptionLabel(stringResource(R.string.paper_size))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                PaperSize.entries.forEachIndexed { index, paper ->
                    SegmentedButton(
                        selected = current.paper == paper,
                        onClick = { viewModel.update { it.copy(paper = paper) } },
                        shape = SegmentedButtonDefaults.itemShape(index, PaperSize.entries.size),
                    ) {
                        Text(stringResource(if (paper == PaperSize.A4) R.string.paper_a4 else R.string.paper_letter))
                    }
                }
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }

            val page = preview
            Box(
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                if (page != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp,
                    ) {
                        Image(
                            bitmap = page.firstPage.asImageBitmap(),
                            contentDescription = stringResource(R.string.cards_preview),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(page.firstPage.width.toFloat() / page.firstPage.height),
                        )
                    }
                } else if (error == null) {
                    CircularProgressIndicator(Modifier.padding(48.dp))
                }
            }
            page?.let {
                Text(
                    pluralStringResource(R.plurals.card_count, it.cardCount, it.cardCount) + " · " +
                        pluralStringResource(R.plurals.page_count, it.pageCount, it.pageCount),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Text(
                stringResource(R.string.printing_tips),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

@Composable
private fun NumberField(label: String, value: Int, onValue: (Int) -> Unit, modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf(value.toString()) }
    OutlinedTextField(
        value = text,
        onValueChange = { input ->
            text = input.filter(Char::isDigit).take(3)
            text.toIntOrNull()?.takeIf { CardNumbers.isValid(it) }?.let(onValue)
        },
        isError = text.toIntOrNull()?.let { !CardNumbers.isValid(it) } ?: true,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier,
    )
}
