package com.ht.intelza.ui

import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ht.intelza.ui.cards.PrintCardsScreen
import com.ht.intelza.ui.classes.ClassDetailScreen
import com.ht.intelza.ui.classes.ClassListScreen
import com.ht.intelza.ui.common.LocalOpenDrawer
import com.ht.intelza.ui.home.HomeScreen
import com.ht.intelza.ui.navigation.Routes
import com.ht.intelza.ui.navigation.AppDrawer
import com.ht.intelza.ui.navigation.navKey
import com.ht.intelza.ui.navigation.navigateFromMenu
import com.ht.intelza.ui.questions.QuestionEditScreen
import com.ht.intelza.ui.questions.SubjectScreen
import com.ht.intelza.ui.questions.SubjectsScreen
import com.ht.intelza.ui.questions.TopicScreen
import com.ht.intelza.ui.reports.ClassReportScreen
import com.ht.intelza.ui.reports.ReportsScreen
import com.ht.intelza.ui.reports.SessionReportScreen
import com.ht.intelza.ui.reports.StudentReportScreen
import com.ht.intelza.ui.reports.TopicReportScreen
import com.ht.intelza.ui.scan.TestCardsScreen
import com.ht.intelza.ui.session.SessionNewScreen
import com.ht.intelza.ui.session.SessionScreen
import com.ht.intelza.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

private fun longArg(name: String): NamedNavArgument = navArgument(name) { type = NavType.LongType }

private fun optionalLongArg(name: String): NamedNavArgument = navArgument(name) {
    type = NavType.LongType
    defaultValue = -1L
}

@Composable
fun IntelzaApp() {
    val nav = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    // Swiping the menu open would fight with the camera screens, so only the button opens it there.
    val swipeToOpen = currentRoute != Routes.SESSION && currentRoute != Routes.TEST_CARDS

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = swipeToOpen || drawerState.isOpen,
        drawerContent = {
            AppDrawer(
                currentKey = backStackEntry?.navKey(),
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    nav.navigateFromMenu(route)
                },
            )
        },
    ) {
        CompositionLocalProvider(
            LocalOpenDrawer provides {
                scope.launch { drawerState.open() }
                Unit
            },
        ) {
            AppNavHost(nav)
        }
    }
}

@Composable
private fun AppNavHost(nav: NavHostController) {
    val back: () -> Unit = { nav.popBackStack() }
    val openSettings = { nav.navigate(Routes.SETTINGS) }
    val openStudentReport = { id: Long -> nav.navigate(Routes.studentReport(id)) }
    val openSessionReport = { id: Long -> nav.navigate(Routes.sessionReport(id)) }

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartEvaluation = { nav.navigate(Routes.newSession()) },
                onContinueSession = { nav.navigate(Routes.session(it)) },
                onOpenReport = openSessionReport,
                onPrintCards = { nav.navigate(Routes.printCards()) },
                onTestCards = { nav.navigate(Routes.TEST_CARDS) },
                onSettings = openSettings,
                onOpenClasses = { nav.navigateFromMenu(Routes.CLASSES) },
                onOpenQuestions = { nav.navigateFromMenu(Routes.QUESTIONS) },
            )
        }

        // Classes and cards
        composable(Routes.CLASSES) {
            ClassListScreen(
                onOpenClass = { nav.navigate(Routes.classDetail(it)) },
                onPrintNumberedCards = { nav.navigate(Routes.printCards()) },
                onSettings = openSettings,
            )
        }
        composable(Routes.CLASS_DETAIL, arguments = listOf(longArg("classId"))) {
            ClassDetailScreen(
                onBack = back,
                onPrintCards = { nav.navigate(Routes.printCards(it)) },
                onStartEvaluation = { nav.navigate(Routes.newSession(classId = it)) },
                onClassReport = { nav.navigate(Routes.classReport(it)) },
                onStudentReport = openStudentReport,
            )
        }
        composable(Routes.PRINT_CARDS, arguments = listOf(optionalLongArg("classId"))) {
            PrintCardsScreen(onBack = back)
        }
        composable(Routes.TEST_CARDS) {
            TestCardsScreen(onBack = back)
        }

        // Question bank
        composable(Routes.QUESTIONS) {
            SubjectsScreen(
                onOpenSubject = { nav.navigate(Routes.subject(it)) },
                onSettings = openSettings,
            )
        }
        composable(Routes.SUBJECT, arguments = listOf(longArg("subjectId"))) {
            SubjectScreen(onBack = back, onOpenTopic = { nav.navigate(Routes.topic(it)) })
        }
        composable(Routes.TOPIC, arguments = listOf(longArg("topicId"))) {
            TopicScreen(
                onBack = back,
                onAddQuestion = { topicId -> nav.navigate(Routes.questionEdit(topicId)) },
                onEditQuestion = { topicId, questionId -> nav.navigate(Routes.questionEdit(topicId, questionId)) },
                onStartEvaluation = { nav.navigate(Routes.newSession(topicId = it)) },
                onTopicReport = { nav.navigate(Routes.topicReport(it)) },
            )
        }
        composable(
            Routes.QUESTION_EDIT,
            arguments = listOf(longArg("topicId"), optionalLongArg("questionId")),
        ) {
            QuestionEditScreen(onBack = back)
        }

        // Evaluation sessions
        composable(
            Routes.SESSION_NEW,
            arguments = listOf(optionalLongArg("classId"), optionalLongArg("topicId")),
        ) {
            SessionNewScreen(
                onBack = back,
                onStarted = { sessionId ->
                    nav.navigate(Routes.session(sessionId)) {
                        popUpTo(Routes.SESSION_NEW) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.SESSION, arguments = listOf(longArg("sessionId"))) {
            SessionScreen(
                onBack = back,
                onFinished = { sessionId ->
                    nav.navigate(Routes.sessionReport(sessionId)) {
                        popUpTo(Routes.SESSION) { inclusive = true }
                    }
                },
            )
        }

        // Reports
        composable(Routes.REPORTS) {
            ReportsScreen(
                onOpenSession = { openSessionReport(it.id) },
                onSettings = openSettings,
            )
        }
        composable(Routes.SESSION_REPORT, arguments = listOf(longArg("sessionId"))) {
            SessionReportScreen(
                onBack = back,
                onContinue = { sessionId ->
                    nav.navigate(Routes.session(sessionId)) {
                        popUpTo(Routes.SESSION_REPORT) { inclusive = true }
                    }
                },
                onStudent = openStudentReport,
            )
        }
        composable(Routes.STUDENT_REPORT, arguments = listOf(longArg("studentId"))) {
            StudentReportScreen(onBack = back, onOpenSession = openSessionReport)
        }
        composable(Routes.TOPIC_REPORT, arguments = listOf(longArg("topicId"))) {
            TopicReportScreen(onBack = back, onOpenSession = openSessionReport)
        }
        composable(Routes.CLASS_REPORT, arguments = listOf(longArg("classId"))) {
            ClassReportScreen(onBack = back, onOpenSession = openSessionReport, onStudent = openStudentReport)
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = back,
                onPrintCards = { nav.navigate(Routes.printCards()) },
                onTestCards = { nav.navigate(Routes.TEST_CARDS) },
            )
        }
    }
}
