package com.ht.intelza.ui.navigation

import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

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

    /** Main sections; opening one from the menu starts a fresh back stack from Home. */
    val ROOTS = setOf(HOME, CLASSES, QUESTIONS, REPORTS)
}

/** Navigates to a destination picked in the navigation menu. */
fun NavController.navigateFromMenu(route: String) {
    if (route in Routes.ROOTS) {
        navigate(route) {
            popUpTo(graph.findStartDestination().id) { inclusive = route == Routes.HOME }
            launchSingleTop = true
        }
    } else {
        navigate(route) { launchSingleTop = true }
    }
}

/** A short key for the screen this entry shows, used to highlight it in the menu. */
fun NavBackStackEntry.navKey(): String {
    val route = destination.route ?: return ""
    fun id(name: String): Long = arguments?.getLong(name, -1L) ?: -1L
    return when (route) {
        Routes.HOME -> "home"
        Routes.CLASSES -> "classes"
        Routes.CLASS_DETAIL -> "class/${id("classId")}"
        Routes.PRINT_CARDS -> id("classId").let { if (it > 0) "cards/$it" else "cards" }
        Routes.QUESTIONS -> "questions"
        Routes.SUBJECT -> "subject/${id("subjectId")}"
        Routes.TOPIC, Routes.QUESTION_EDIT -> "topic/${id("topicId")}"
        Routes.SESSION_NEW -> "new-session"
        Routes.SESSION -> "session/${id("sessionId")}"
        Routes.REPORTS, Routes.SESSION_REPORT, Routes.STUDENT_REPORT, Routes.TOPIC_REPORT -> "reports"
        Routes.CLASS_REPORT -> "classreport/${id("classId")}"
        Routes.SETTINGS -> "settings"
        Routes.TEST_CARDS -> "test-cards"
        else -> route
    }
}
