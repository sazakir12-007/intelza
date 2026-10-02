package com.ht.intelza.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.ht.intelza.R

object Routes {
    const val HOME = "home"
    const val CLASSES = "classes"
    const val CLASS_DETAIL = "classes/{classId}"
    const val PRINT_CARDS = "cards?classId={classId}"
    const val QUESTIONS = "questions"
    const val SUBJECT = "questions/subject/{subjectId}"
    const val TOPIC = "questions/topic/{topicId}"
    const val QUESTION_EDIT = "questions/topic/{topicId}/edit?questionId={questionId}"
    const val SESSION_NEW = "new-session?classId={classId}&topicId={topicId}"
    const val SESSION = "session/{sessionId}"
    const val REPORTS = "reports"
    const val SESSION_REPORT = "reports/session/{sessionId}"
    const val STUDENT_REPORT = "reports/student/{studentId}"
    const val TOPIC_REPORT = "reports/topic/{topicId}"
    const val CLASS_REPORT = "reports/class/{classId}"
    const val SETTINGS = "settings"
    const val TEST_CARDS = "test-cards"

    fun classDetail(classId: Long) = "classes/$classId"
    fun printCards(classId: Long? = null) = if (classId == null) "cards" else "cards?classId=$classId"
    fun subject(subjectId: Long) = "questions/subject/$subjectId"
    fun topic(topicId: Long) = "questions/topic/$topicId"
    fun questionEdit(topicId: Long, questionId: Long? = null) =
        if (questionId == null) "questions/topic/$topicId/edit" else "questions/topic/$topicId/edit?questionId=$questionId"

    fun newSession(classId: Long? = null, topicId: Long? = null): String {
        val params = listOfNotNull(classId?.let { "classId=$it" }, topicId?.let { "topicId=$it" })
        return if (params.isEmpty()) "new-session" else "new-session?" + params.joinToString("&")
    }

    fun session(sessionId: Long) = "session/$sessionId"
    fun sessionReport(sessionId: Long) = "reports/session/$sessionId"
    fun studentReport(studentId: Long) = "reports/student/$studentId"
    fun topicReport(topicId: Long) = "reports/topic/$topicId"
    fun classReport(classId: Long) = "reports/class/$classId"
}

enum class TopLevelDestination(
    val route: String,
    @param:StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    HOME(Routes.HOME, R.string.nav_home, Icons.Outlined.Home, Icons.Filled.Home),
    CLASSES(Routes.CLASSES, R.string.nav_classes, Icons.Outlined.Groups, Icons.Filled.Groups),
    QUESTIONS(Routes.QUESTIONS, R.string.nav_questions, Icons.Outlined.Quiz, Icons.Filled.Quiz),
    REPORTS(Routes.REPORTS, R.string.nav_reports, Icons.Outlined.Insights, Icons.Filled.Insights),
}

fun NavController.navigateTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun AppBottomBar(current: TopLevelDestination, onNavigate: (TopLevelDestination) -> Unit) {
    NavigationBar {
        for (destination in TopLevelDestination.entries) {
            val selected = destination == current
            NavigationBarItem(
                selected = selected,
                onClick = { if (!selected) onNavigate(destination) },
                icon = {
                    Icon(if (selected) destination.selectedIcon else destination.icon, contentDescription = null)
                },
                label = { Text(stringResource(destination.label)) },
            )
        }
    }
}
