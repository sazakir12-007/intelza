package com.ht.intelza.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.BrightnessAuto
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Topic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.ThemeMode
import com.ht.intelza.data.db.ClassSummary
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.data.db.SubjectSummary
import com.ht.intelza.data.db.TopicWithSubject
import com.ht.intelza.ui.common.BrandLockup
import com.ht.intelza.ui.common.appViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DrawerData(
    val classes: List<ClassSummary>,
    val subjects: List<SubjectSummary>,
    val topics: List<TopicWithSubject>,
    val inProgress: List<SessionOverview>,
    val themeMode: ThemeMode,
)

class DrawerViewModel(
    classes: ClassRepository,
    questions: QuestionRepository,
    reports: ReportRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val data: StateFlow<DrawerData?> = combine(
        classes.observeSummaries(),
        questions.observeSubjects(),
        questions.observeAllTopics(),
        reports.observeUnfinishedSessions(),
        settings.settings,
    ) { classList, subjects, topics, inProgress, appSettings ->
        DrawerData(
            classes = classList.filterNot { it.schoolClass.archived },
            subjects = subjects,
            topics = topics,
            inProgress = inProgress,
            themeMode = appSettings.themeMode,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.update { it.copy(themeMode = mode) } }
    }
}

/** One entry in the navigation tree. */
private data class NavNode(
    /** Unique within the tree; also used to remember which branches are open. */
    val id: String,
    val label: String,
    /** Destination, or null for a group that only opens and closes. */
    val route: String? = null,
    /** Matches [navKey] of the screen this entry opens, to highlight it. */
    val selectionKey: String? = null,
    val icon: ImageVector? = null,
    val children: List<NavNode> = emptyList(),
    val trailing: String? = null,
)

/**
 * The sliding navigation menu: every screen of the app as a tree (classes with their
 * tools, subjects with their topics, reports, tools and settings).
 */
@Composable
fun AppDrawer(currentKey: String?, onNavigate: (String) -> Unit) {
    val viewModel = appViewModel { c, _ -> DrawerViewModel(c.classes, c.questions, c.reports, c.settings) }
    val data by viewModel.data.collectAsStateWithLifecycle()
    val tree = data?.let { buildTree(it) }.orEmpty()

    val expanded: MutableState<Set<String>> = rememberSaveable(
        saver = listSaver(save = { it.value.toList() }, restore = { mutableStateOf(it.toSet()) }),
    ) { mutableStateOf(setOf(GROUP_CLASSES, GROUP_QUESTIONS, GROUP_TOOLS)) }

    // Open the branches leading to the current screen.
    LaunchedEffect(currentKey, data != null) {
        val path = currentKey?.let { key -> pathTo(tree, key) }.orEmpty()
        if (path.isNotEmpty()) expanded.value = expanded.value + path
    }

    ModalDrawerSheet(Modifier.width(304.dp)) {
        Column(Modifier.fillMaxHeight()) {
            BrandLockup(Modifier.padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 12.dp))
            HorizontalDivider()
            LazyColumn(Modifier.weight(1f).padding(vertical = 4.dp)) {
                val rows = flatten(tree, 0, expanded.value)
                items(rows, key = { it.first.id }) { (node, level) ->
                    TreeRow(
                        node = node,
                        level = level,
                        selected = node.selectionKey != null && node.selectionKey == currentKey,
                        isExpanded = node.id in expanded.value,
                        onToggle = {
                            expanded.value = if (node.id in expanded.value) {
                                expanded.value - node.id
                            } else {
                                expanded.value + node.id
                            }
                        },
                        onOpen = { node.route?.let(onNavigate) },
                    )
                }
            }
            HorizontalDivider()
            ThemeSwitch(
                mode = data?.themeMode ?: ThemeMode.SYSTEM,
                onChange = viewModel::setThemeMode,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(12.dp),
            )
        }
    }
}

@Composable
private fun buildTree(data: DrawerData): List<NavNode> {
    val topicsBySubject = data.topics.groupBy { it.topic.subjectId }
    return listOf(
        NavNode(
            id = "home",
            label = stringResource(R.string.nav_home),
            route = Routes.HOME,
            selectionKey = "home",
            icon = Icons.Outlined.Home,
        ),
        NavNode(
            id = "start",
            label = stringResource(R.string.start_evaluation),
            route = Routes.newSession(),
            selectionKey = "new-session",
            icon = Icons.Outlined.PlayCircle,
        ),
        NavNode(
            id = GROUP_CLASSES,
            label = stringResource(R.string.nav_classes),
            route = Routes.CLASSES,
            selectionKey = "classes",
            icon = Icons.Outlined.Groups,
            trailing = data.classes.size.toString(),
            children = data.classes.map { summary ->
                val id = summary.schoolClass.id
                NavNode(
                    id = "class-$id",
                    label = summary.schoolClass.name,
                    route = Routes.classDetail(id),
                    selectionKey = "class/$id",
                    trailing = summary.studentCount.toString(),
                    children = listOf(
                        NavNode(
                            id = "class-$id-cards",
                            label = stringResource(R.string.print_cards),
                            route = Routes.printCards(id),
                            selectionKey = "cards/$id",
                            icon = Icons.Outlined.Print,
                        ),
                        NavNode(
                            id = "class-$id-start",
                            label = stringResource(R.string.start_evaluation),
                            route = Routes.newSession(classId = id),
                            icon = Icons.Outlined.PlayArrow,
                        ),
                        NavNode(
                            id = "class-$id-report",
                            label = stringResource(R.string.class_report),
                            route = Routes.classReport(id),
                            selectionKey = "classreport/$id",
                            icon = Icons.Outlined.Assessment,
                        ),
                    ),
                )
            },
        ),
        NavNode(
            id = GROUP_QUESTIONS,
            label = stringResource(R.string.question_bank),
            route = Routes.QUESTIONS,
            selectionKey = "questions",
            icon = Icons.Outlined.Quiz,
            children = data.subjects.map { summary ->
                val subjectId = summary.subject.id
                NavNode(
                    id = "subject-$subjectId",
                    label = summary.subject.name,
                    route = Routes.subject(subjectId),
                    selectionKey = "subject/$subjectId",
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    trailing = summary.topicCount.toString(),
                    children = topicsBySubject[subjectId].orEmpty().map { topic ->
                        NavNode(
                            id = "topic-${topic.topic.id}",
                            label = topicLabel(topic),
                            route = Routes.topic(topic.topic.id),
                            selectionKey = "topic/${topic.topic.id}",
                            icon = Icons.Outlined.Topic,
                            trailing = topic.questionCount.toString(),
                        )
                    },
                )
            },
        ),
        NavNode(
            id = GROUP_REPORTS,
            label = stringResource(R.string.nav_reports),
            route = Routes.REPORTS,
            selectionKey = "reports",
            icon = Icons.Outlined.Insights,
            children = data.inProgress.take(MAX_IN_PROGRESS).map { session ->
                NavNode(
                    id = "session-${session.id}",
                    label = stringResource(
                        R.string.drawer_continue,
                        session.title.ifEmpty { stringResource(R.string.quick_session) },
                        session.className,
                    ),
                    route = Routes.session(session.id),
                    selectionKey = "session/${session.id}",
                    icon = Icons.Outlined.PlayCircle,
                )
            } + data.classes.map { summary ->
                val id = summary.schoolClass.id
                NavNode(
                    id = "report-class-$id",
                    label = stringResource(R.string.drawer_class_report, summary.schoolClass.name),
                    route = Routes.classReport(id),
                    selectionKey = "classreport/$id",
                    icon = Icons.Outlined.Assessment,
                )
            },
        ),
        NavNode(
            id = GROUP_TOOLS,
            label = stringResource(R.string.tools),
            icon = Icons.Outlined.Build,
            children = listOf(
                NavNode(
                    id = "tool-test-cards",
                    label = stringResource(R.string.test_cards_title),
                    route = Routes.TEST_CARDS,
                    selectionKey = "test-cards",
                    icon = Icons.Outlined.QrCodeScanner,
                ),
                NavNode(
                    id = "tool-print",
                    label = stringResource(R.string.print_numbered_cards),
                    route = Routes.printCards(),
                    selectionKey = "cards",
                    icon = Icons.Outlined.Print,
                ),
            ),
        ),
        NavNode(
            id = "settings",
            label = stringResource(R.string.settings),
            route = Routes.SETTINGS,
            selectionKey = "settings",
            icon = Icons.Outlined.Settings,
        ),
    )
}

@Composable
private fun topicLabel(topic: TopicWithSubject): String =
    if (topic.topic.grade.isBlank()) {
        topic.topic.name
    } else {
        stringResource(R.string.drawer_topic_with_grade, topic.topic.name, topic.topic.grade)
    }

@Composable
private fun TreeRow(
    node: NavNode,
    level: Int,
    selected: Boolean,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    val hasChildren = node.children.isNotEmpty()
    val background = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val content = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(50))
            .background(background)
            .clickable { if (node.route != null) onOpen() else onToggle() }
            .height(if (level == 0) 42.dp else 38.dp)
            .padding(start = (12 + level * 18).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (node.icon != null) {
            Icon(
                node.icon,
                contentDescription = null,
                tint = if (selected) content else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (level == 0) 22.dp else 18.dp),
            )
        }
        Text(
            node.label,
            style = if (level == 0) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (level == 0 || selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        node.trailing?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (hasChildren) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (isExpanded) R.string.collapse else R.string.expand),
                    modifier = Modifier.rotate(if (isExpanded) 0f else -90f),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            Spacer(Modifier.width(32.dp))
        }
    }
}

@Composable
private fun ThemeSwitch(mode: ThemeMode, onChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val options = listOf(
        Triple(ThemeMode.SYSTEM, Icons.Outlined.BrightnessAuto, R.string.theme_system),
        Triple(ThemeMode.LIGHT, Icons.Outlined.LightMode, R.string.theme_light),
        Triple(ThemeMode.DARK, Icons.Outlined.DarkMode, R.string.theme_dark),
    )
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, icon, label) ->
            SegmentedButton(
                selected = mode == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) },
            ) { Text(stringResource(label), maxLines = 1) }
        }
    }
}

private fun flatten(nodes: List<NavNode>, level: Int, expanded: Set<String>): List<Pair<NavNode, Int>> =
    nodes.flatMap { node ->
        listOf(node to level) +
            if (node.id in expanded) flatten(node.children, level + 1, expanded) else emptyList()
    }

/** Ids of the branches that contain the entry for [key], so they can be opened. */
private fun pathTo(nodes: List<NavNode>, key: String): List<String> {
    for (node in nodes) {
        if (node.selectionKey == key) return listOf(node.id)
        val below = pathTo(node.children, key)
        if (below.isNotEmpty()) return listOf(node.id) + below
    }
    return emptyList()
}

private const val GROUP_CLASSES = "classes"
private const val GROUP_QUESTIONS = "questions"
private const val GROUP_REPORTS = "reports"
private const val GROUP_TOOLS = "tools"
private const val MAX_IN_PROGRESS = 5
