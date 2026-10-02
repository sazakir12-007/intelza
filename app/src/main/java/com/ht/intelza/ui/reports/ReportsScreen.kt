package com.ht.intelza.ui.reports

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.AppSettings
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.ui.common.EmptyState
import com.ht.intelza.ui.common.SectionHeader
import com.ht.intelza.ui.common.appViewModel
import com.ht.intelza.ui.common.formatDate
import com.ht.intelza.ui.navigation.AppBottomBar
import com.ht.intelza.ui.navigation.TopLevelDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ReportsViewModel(reports: ReportRepository, settings: SettingsRepository) : ViewModel() {
    val sessions: StateFlow<List<SessionOverview>?> = reports.observeSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())
}

@Composable
fun ReportsScreen(
    onOpenSession: (SessionOverview) -> Unit,
    onSettings: () -> Unit,
    onNavigate: (TopLevelDestination) -> Unit,
) {
    val viewModel = appViewModel { c, _ -> ReportsViewModel(c.reports, c.settings) }
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_reports)) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        bottomBar = { AppBottomBar(TopLevelDestination.REPORTS, onNavigate) },
    ) { padding ->
        val list = sessions ?: return@Scaffold
        val inProgress = list.filter { it.finishedAt == null }
        val finished = list.filter { it.finishedAt != null }
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(bottom = 24.dp)) {
            if (list.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Outlined.Insights,
                        title = stringResource(R.string.no_reports_title),
                        message = stringResource(R.string.no_reports_message),
                    )
                }
            }
            if (inProgress.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.in_progress)) }
                items(inProgress, key = { it.id }) { session ->
                    SessionRow(session, settings, onClick = { onOpenSession(session) })
                    HorizontalDivider()
                }
            }
            if (finished.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.completed)) }
                items(finished, key = { it.id }) { session ->
                    SessionRow(session, settings, onClick = { onOpenSession(session) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
fun SessionRow(session: SessionOverview, settings: AppSettings, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(session.title.ifEmpty { stringResource(R.string.quick_session) }) },
        supportingContent = {
            Text(
                listOfNotNull(
                    session.className,
                    session.subjectName.takeIf { it.isNotBlank() },
                    formatDate(session.startedAt),
                ).joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = { ScoreChip(session.average, settings) },
    )
}
