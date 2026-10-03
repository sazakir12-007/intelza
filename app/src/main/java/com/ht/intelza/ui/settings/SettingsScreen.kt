package com.ht.intelza.ui.settings

import android.app.Application
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import com.ht.intelza.data.SampleDataLoader
import com.ht.intelza.data.ThemeMode
import com.ht.intelza.data.ThemePalette
import com.ht.intelza.ui.theme.PaletteSwatches
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.BuildConfig
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.BackupManager
import com.ht.intelza.data.CardSize
import com.ht.intelza.data.PaperSize
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.export.Sharing
import com.ht.intelza.ui.common.BrandLockup
import com.ht.intelza.ui.common.ConfirmDialog
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import com.ht.intelza.ui.common.AppTopBar
import com.ht.intelza.ui.common.CompactListItem

class SettingsViewModel(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    private val backups: BackupManager,
    private val sampleData: SampleDataLoader,
) : ViewModel() {
    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var lastBackup: File? = null

    fun update(transform: (AppSettings) -> AppSettings) = viewModelScope.launch { settingsRepository.update(transform) }

    fun backupFileName() = app.getString(R.string.backup_file_name, LocalDate.now().toString()) + ".intelza"

    /** Creates a backup, then hands it to [onReady] (to save or share). */
    fun createBackup(onReady: (File) -> Unit) = runBusy {
        val file = backups.createBackup(backupFileName())
        lastBackup = file
        onReady(file)
    }

    fun saveBackupTo(destination: Uri) = runBusy {
        val file = lastBackup ?: backups.createBackup(backupFileName())
        backups.copyTo(file, destination)
        _message.value = app.getString(R.string.backup_saved)
    }

    fun restore(source: Uri) = runBusy {
        backups.restore(source)
        backups.restartApp()
    }

    fun addSampleData() = runBusy {
        val added = sampleData.add()
        _message.value = if (added.isEmpty) {
            app.getString(R.string.sample_data_already)
        } else {
            app.getString(R.string.sample_data_added, added.classes, added.students, added.topics, added.questions)
        }
    }

    fun removeSampleData() = runBusy {
        val removed = sampleData.remove()
        _message.value = app.getString(if (removed > 0) R.string.sample_data_removed else R.string.sample_data_none)
    }

    fun messageShown() {
        _message.value = null
    }

    private fun runBusy(block: suspend () -> Unit) = viewModelScope.launch {
        _busy.value = true
        try {
            block()
        } catch (e: BackupManager.InvalidBackupException) {
            _message.value = app.getString(R.string.restore_invalid)
        } catch (e: Exception) {
            _message.value = app.getString(R.string.backup_failed)
        } finally {
            _busy.value = false
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onPrintCards: () -> Unit,
    onTestCards: () -> Unit,
) {
    val viewModel = appViewModel { c, _ -> SettingsViewModel(c.application, c.settings, c.backups, c.sampleData) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showBackupChoice by rememberSaveable { mutableStateOf(false) }
    var showLicences by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    var confirmRemoveSample by rememberSaveable { mutableStateOf(false) }
    val shareTitle = stringResource(R.string.share_backup)

    val saveBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let(viewModel::saveBackupTo)
    }
    val pickRestore = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val current = settings ?: return@Scaffold
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item { SectionHeader(stringResource(R.string.settings_appearance)) }
            item {
                AppearanceSettings(
                    mode = current.themeMode,
                    palette = current.themePalette,
                    onMode = { mode -> viewModel.update { it.copy(themeMode = mode) } },
                    onPalette = { palette -> viewModel.update { it.copy(themePalette = palette) } },
                )
            }

            item { SectionHeader(stringResource(R.string.settings_results)) }
            item {
                ThresholdSetting(
                    title = stringResource(R.string.needs_help_threshold),
                    description = stringResource(R.string.needs_help_threshold_hint),
                    value = current.needsHelpThreshold,
                    onChange = { v -> viewModel.update { it.copy(needsHelpThreshold = v) } },
                )
            }
            item {
                ThresholdSetting(
                    title = stringResource(R.string.reteach_threshold),
                    description = stringResource(R.string.reteach_threshold_hint),
                    value = current.reteachThreshold,
                    onChange = { v -> viewModel.update { it.copy(reteachThreshold = v) } },
                )
            }

            item { SectionHeader(stringResource(R.string.settings_scanning)) }
            item {
                SwitchSetting(stringResource(R.string.scan_sound), null, current.scanSound) { on ->
                    viewModel.update { it.copy(scanSound = on) }
                }
            }
            item {
                SwitchSetting(stringResource(R.string.scan_vibration), null, current.scanVibration) { on ->
                    viewModel.update { it.copy(scanVibration = on) }
                }
            }
            item {
                SwitchSetting(
                    stringResource(R.string.show_names_while_scanning),
                    stringResource(R.string.show_names_while_scanning_hint),
                    current.showNamesWhileScanning,
                ) { on -> viewModel.update { it.copy(showNamesWhileScanning = on) } }
            }

            item { SectionHeader(stringResource(R.string.settings_cards)) }
            item {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.card_size), style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        CardSize.entries.forEachIndexed { index, size ->
                            SegmentedButton(
                                selected = current.cardSize == size,
                                onClick = { viewModel.update { it.copy(cardSize = size) } },
                                shape = SegmentedButtonDefaults.itemShape(index, CardSize.entries.size),
                            ) {
                                Text(stringResource(if (size == CardSize.STANDARD) R.string.card_size_standard else R.string.card_size_large))
                            }
                        }
                    }
                    Text(stringResource(R.string.paper_size), style = MaterialTheme.typography.bodyLarge)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        PaperSize.entries.forEachIndexed { index, paper ->
                            SegmentedButton(
                                selected = current.paperSize == paper,
                                onClick = { viewModel.update { it.copy(paperSize = paper) } },
                                shape = SegmentedButtonDefaults.itemShape(index, PaperSize.entries.size),
                            ) {
                                Text(stringResource(if (paper == PaperSize.A4) R.string.paper_a4 else R.string.paper_letter))
                            }
                        }
                    }
                }
            }
            item {
                SwitchSetting(
                    stringResource(R.string.print_student_names),
                    stringResource(R.string.print_student_names_hint),
                    current.printNamesOnCards,
                ) { on -> viewModel.update { it.copy(printNamesOnCards = on) } }
            }
            item {
                ActionSetting(Icons.Outlined.Print, stringResource(R.string.print_numbered_cards), stringResource(R.string.print_numbered_cards_hint), onPrintCards)
            }
            item {
                ActionSetting(Icons.Outlined.QrCodeScanner, stringResource(R.string.test_cards_title), stringResource(R.string.test_cards_summary), onTestCards)
            }

            item { SectionHeader(stringResource(R.string.settings_data)) }
            item {
                ActionSetting(Icons.Outlined.Backup, stringResource(R.string.backup), stringResource(R.string.backup_hint)) {
                    showBackupChoice = true
                }
            }
            item {
                ActionSetting(Icons.Outlined.Restore, stringResource(R.string.restore), stringResource(R.string.restore_hint)) {
                    pickRestore.launch(arrayOf("*/*"))
                }
            }
            if (busy) {
                item {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.settings_sample_data)) }
            item {
                ActionSetting(
                    Icons.Outlined.School,
                    stringResource(R.string.sample_data_add),
                    stringResource(R.string.sample_data_add_hint),
                    viewModel::addSampleData,
                )
            }
            item {
                ActionSetting(
                    Icons.Outlined.DeleteSweep,
                    stringResource(R.string.sample_data_remove),
                    stringResource(R.string.sample_data_remove_hint),
                ) { confirmRemoveSample = true }
            }

            item { SectionHeader(stringResource(R.string.settings_about)) }
            item {
                ActionSetting(Icons.Outlined.Policy, stringResource(R.string.privacy), stringResource(R.string.privacy_summary)) {
                    showPrivacy = true
                }
            }
            item {
                ActionSetting(Icons.Outlined.Info, stringResource(R.string.open_source_licences), null) {
                    showLicences = true
                }
            }
            item {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BrandLockup()
                    Text(
                        stringResource(R.string.version_value, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    if (showBackupChoice) {
        AlertDialog(
            onDismissRequest = { showBackupChoice = false },
            title = { Text(stringResource(R.string.backup)) },
            text = { Text(stringResource(R.string.backup_choice_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    viewModel.createBackup { saveBackup.launch(it.name) }
                }) { Text(stringResource(R.string.save_to_device)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showBackupChoice = false
                    viewModel.createBackup { Sharing.share(context, it, "application/zip", shareTitle) }
                }) { Text(stringResource(R.string.share)) }
            },
        )
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.restore_confirm_title),
            message = stringResource(R.string.restore_confirm_message),
            confirmLabel = stringResource(R.string.restore),
            onDismiss = { pendingRestore = null },
            onConfirm = {
                pendingRestore = null
                viewModel.restore(uri)
            },
        )
    }

    if (confirmRemoveSample) {
        ConfirmDialog(
            title = stringResource(R.string.sample_data_remove_title),
            message = stringResource(R.string.sample_data_remove_message),
            confirmLabel = stringResource(R.string.remove),
            onDismiss = { confirmRemoveSample = false },
            onConfirm = {
                confirmRemoveSample = false
                viewModel.removeSampleData()
            },
        )
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.privacy)) },
            text = { Text(stringResource(R.string.privacy_details)) },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.ok)) } },
        )
    }

    if (showLicences) {
        AlertDialog(
            onDismissRequest = { showLicences = false },
            title = { Text(stringResource(R.string.open_source_licences)) },
            text = {
                Column(
                    Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(stringResource(R.string.apriltag_licence), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showLicences = false }) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun ThresholdSetting(title: String, description: String, value: Int, onChange: (Int) -> Unit) {
    var local by remember(value) { mutableFloatStateOf(value.toFloat()) }
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text("${local.toInt()}%", style = MaterialTheme.typography.titleMedium)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(
            value = local,
            onValueChange = { local = it },
            onValueChangeFinished = { onChange(local.toInt()) },
            valueRange = 0f..100f,
            steps = 19,
        )
    }
}

@Composable
private fun SwitchSetting(title: String, description: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    CompactListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = if (description != null) { { Text(description) } } else null,
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun ActionSetting(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String?,
    onClick: () -> Unit,
) {
    CompactListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = if (description != null) { { Text(description) } } else null,
    )
}

@Composable
private fun AppearanceSettings(
    mode: ThemeMode,
    palette: ThemePalette,
    onMode: (ThemeMode) -> Unit,
    onPalette: (ThemePalette) -> Unit,
) {
    Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.bodyLarge)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { index, value ->
                SegmentedButton(
                    selected = mode == value,
                    onClick = { onMode(value) },
                    shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                ) {
                    Text(
                        stringResource(
                            when (value) {
                                ThemeMode.SYSTEM -> R.string.theme_system
                                ThemeMode.LIGHT -> R.string.theme_light
                                ThemeMode.DARK -> R.string.theme_dark
                            },
                        ),
                    )
                }
            }
        }
        Text(stringResource(R.string.accent_colour), style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            for (option in ThemePalette.entries) {
                if (option == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) continue
                PaletteChoice(option, selected = option == palette, onClick = { onPalette(option) })
            }
        }
    }
}

@Composable
private fun PaletteChoice(palette: ThemePalette, selected: Boolean, onClick: () -> Unit) {
    val label = stringResource(
        when (palette) {
            ThemePalette.INDIGO -> R.string.palette_indigo
            ThemePalette.TEAL -> R.string.palette_teal
            ThemePalette.PURPLE -> R.string.palette_purple
            ThemePalette.ORANGE -> R.string.palette_orange
            ThemePalette.DYNAMIC -> R.string.palette_wallpaper
        },
    )
    val fill = PaletteSwatches[palette]?.let { SolidColor(it) } ?: Brush.sweepGradient(
        listOf(Color(0xFFE53935), Color(0xFFFDD835), Color(0xFF43A047), Color(0xFF1E88E5), Color(0xFFE53935)),
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(4.dp),
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(fill)
                .border(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

