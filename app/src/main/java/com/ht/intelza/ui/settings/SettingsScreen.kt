package com.ht.intelza.ui.settings

import android.app.Application
import android.net.Uri
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
import androidx.compose.material3.ListItem
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
import androidx.compose.material3.TopAppBar
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

class SettingsViewModel(
    private val app: Application,
    private val settingsRepository: SettingsRepository,
    private val backups: BackupManager,
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
    val viewModel = appViewModel { c, _ -> SettingsViewModel(c.application, c.settings, c.backups) }
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var pendingRestore by rememberSaveable { mutableStateOf<Uri?>(null) }
    var showBackupChoice by rememberSaveable { mutableStateOf(false) }
    var showLicences by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
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
            TopAppBar(
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
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator()
                    }
                }
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
                Text(
                    stringResource(R.string.version_value, BuildConfig.VERSION_NAME),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
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
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
    ListItem(
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
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { Icon(icon, null) },
        headlineContent = { Text(title) },
        supportingContent = if (description != null) { { Text(description) } } else null,
    )
}
