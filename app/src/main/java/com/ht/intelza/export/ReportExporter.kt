package com.ht.intelza.export

import android.content.Context
import com.ht.intelza.R
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.ReportRepository
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.data.csv.Csv
import com.ht.intelza.data.db.SessionOverview
import com.ht.intelza.data.db.SessionQuestionEntity
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.ClassProgress
import com.ht.intelza.domain.QuestionType
import com.ht.intelza.domain.SessionReport
import com.ht.intelza.ui.common.formatDate
import com.ht.intelza.ui.common.percentValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Builds the PDF and CSV reports teachers can share (requirement F5). */
class ReportExporter(
    private val context: Context,
    private val reports: ReportRepository,
    private val classes: ClassRepository,
    private val settings: SettingsRepository,
) {
    suspend fun sessionCsv(sessionId: Long): File = withContext(Dispatchers.IO) {
        val overview = requireNotNull(reports.getSessionOverview(sessionId))
        val report = reports.getSessionReport(sessionId)
        val questions = report.questions.map { it.question }
        val rows = mutableListOf<List<Any?>>()
        rows += listOf(string(R.string.csv_card), string(R.string.csv_roll), string(R.string.csv_name), string(R.string.csv_attendance)) +
            questions.indices.map { "Q${it + 1}" } +
            listOf(string(R.string.csv_correct), string(R.string.csv_score))
        for (result in report.students) {
            val student = result.student
            rows += listOf(student.cardNumber, student.rollNumber, student.name, string(R.string.present)) +
                questions.map { result.answers[it.id]?.name.orEmpty() } +
                listOf(result.correct, result.score?.let { "${percentValue(it)}%" }.orEmpty())
        }
        for (student in report.absent) {
            rows += listOf(student.cardNumber, student.rollNumber, student.name, string(R.string.absent)) +
                questions.map { "" } + listOf("", "")
        }
        rows += emptyList<Any?>()
        rows += listOf(
            string(R.string.csv_question), string(R.string.csv_text), string(R.string.csv_type),
            string(R.string.csv_answer), string(R.string.csv_percent_correct),
            "A", "B", "C", "D", string(R.string.no_answer),
        )
        report.questions.forEachIndexed { index, result ->
            val q = result.question
            rows += listOf(
                "Q${index + 1}",
                q.text,
                typeName(q.type),
                q.correct?.let { answerName(q, it) }.orEmpty(),
                result.fractionCorrect?.let { "${percentValue(it)}%" }.orEmpty(),
            ) + AnswerOption.entries.map { option ->
                if (option in result.counts) result.counts[option] else ""
            } + listOf(result.noAnswer.size)
        }
        writeBytes(baseName(overview) + ".csv") { Csv.write(rows).toByteArray(Charsets.UTF_8) }
    }

    suspend fun sessionPdf(sessionId: Long): File = withContext(Dispatchers.IO) {
        val overview = requireNotNull(reports.getSessionOverview(sessionId))
        val report = reports.getSessionReport(sessionId)
        val current = settings.current()
        val writer = PdfReportWriter(footer = "Intelza · ${overview.className} · ${formatDate(overview.startedAt)}")
        writer.title(sessionTitle(overview))
        writer.subtitle(
            listOfNotNull(
                overview.className,
                overview.subjectName.takeIf { it.isNotBlank() },
                formatDate(overview.startedAt),
            ).joinToString(" · "),
        )
        writer.stats(
            listOf(
                string(R.string.class_average) to (report.classAverage?.let { "${percentValue(it)}%" } ?: "–"),
                string(R.string.students_present) to "${report.students.size}/${report.students.size + report.absent.size}",
                string(R.string.questions_asked) to report.questions.size.toString(),
            ),
        )
        addFlags(writer, report, current.needsHelpThreshold, current.reteachThreshold)

        writer.heading(string(R.string.students))
        writer.table(
            header = listOf(string(R.string.csv_card), string(R.string.csv_name), string(R.string.csv_correct), string(R.string.csv_score)),
            rows = report.students.map { result ->
                listOf(
                    result.student.cardNumber.toString(),
                    result.student.name,
                    "${result.correct}/${result.scoredQuestions}",
                    result.score?.let { "${percentValue(it)}%" } ?: "–",
                )
            },
            weights = listOf(1f, 5f, 2f, 2f),
            highlight = { index ->
                val score = report.students[index].score
                if (score != null && score * 100 < current.needsHelpThreshold) LIGHT_RED else null
            },
        )

        writer.heading(string(R.string.questions))
        writer.table(
            header = listOf("#", string(R.string.csv_question), string(R.string.csv_answer), string(R.string.csv_percent_correct), "A", "B", "C", "D", "–"),
            rows = report.questions.mapIndexed { index, result ->
                val q = result.question
                listOf(
                    "${index + 1}",
                    q.text.ifBlank { string(R.string.picture_question) },
                    q.correct?.let { answerName(q, it) } ?: string(R.string.type_poll),
                    result.fractionCorrect?.let { "${percentValue(it)}%" } ?: "–",
                ) + AnswerOption.entries.map { option -> result.counts[option]?.toString() ?: "" } +
                    listOf(result.noAnswer.size.toString())
            },
            weights = listOf(0.6f, 6f, 1.6f, 1.4f, 0.6f, 0.6f, 0.6f, 0.6f, 0.6f),
            highlight = { index ->
                val fraction = report.questions[index].fractionCorrect
                if (fraction != null && fraction * 100 < current.reteachThreshold) LIGHT_AMBER else null
            },
        )

        if (report.absent.isNotEmpty()) {
            writer.heading(string(R.string.absent))
            writer.paragraph(report.absent.joinToString(", ") { it.name })
        }
        writeStream(baseName(overview) + ".pdf") { out -> writer.write(out) }
    }

    /** Students by sessions, with each student's score per session and overall. */
    suspend fun classCsv(classId: Long): File = withContext(Dispatchers.IO) {
        val schoolClass = requireNotNull(classes.getClass(classId))
        val sessions = reports.getSessionsForClass(classId)
        val progress = ClassProgress.compute(sessions, reports.getClassScores(classId), classes.getAllStudents(classId))
        val rows = mutableListOf<List<Any?>>()
        rows += listOf(string(R.string.csv_card), string(R.string.csv_roll), string(R.string.csv_name)) +
            sessions.map { "${formatDate(it.startedAt)} ${sessionTitle(it)}" } +
            listOf(string(R.string.csv_average))
        for (student in progress) {
            rows += listOf(student.student.cardNumber, student.student.rollNumber, student.student.name) +
                sessions.map { session ->
                    when {
                        session.id in student.absentSessions -> string(R.string.absent)
                        else -> student.scores[session.id]?.let { "${percentValue(it)}%" }.orEmpty()
                    }
                } + listOf(student.average?.let { "${percentValue(it)}%" }.orEmpty())
        }
        rows += listOf("", "", string(R.string.class_average)) +
            sessions.map { s -> s.average?.let { "${percentValue(it)}%" }.orEmpty() } + listOf("")
        writeBytes(context.getString(R.string.class_report_file_name, schoolClass.name) + ".csv") {
            Csv.write(rows).toByteArray(Charsets.UTF_8)
        }
    }

    private fun addFlags(writer: PdfReportWriter, report: SessionReport, needsHelp: Int, reteach: Int) {
        val struggling = report.studentsNeedingHelp(needsHelp)
        writer.heading(context.getString(R.string.needs_help_heading, needsHelp))
        writer.paragraph(
            if (struggling.isEmpty()) {
                string(R.string.nobody_needs_help)
            } else {
                struggling.joinToString(", ") { "${it.student.name} (${it.score?.let(::percentValue)}%)" }
            },
        )
        val hard = report.questionsToReteach(reteach)
        writer.heading(context.getString(R.string.reteach_heading, reteach))
        if (hard.isEmpty()) {
            writer.paragraph(string(R.string.nothing_to_reteach))
        } else {
            for (result in hard) {
                val index = report.questions.indexOf(result) + 1
                writer.paragraph(
                    "Q$index · ${result.fractionCorrect?.let(::percentValue)}% · " +
                        result.question.text.ifBlank { string(R.string.picture_question) },
                )
            }
        }
    }

    private fun sessionTitle(overview: SessionOverview) = overview.title.ifEmpty { string(R.string.quick_session) }

    private fun baseName(overview: SessionOverview) =
        "${sessionTitle(overview)} - ${overview.className} - ${formatDate(overview.startedAt)}"

    private fun typeName(type: QuestionType) = string(
        when (type) {
            QuestionType.MULTIPLE_CHOICE -> R.string.type_multiple_choice
            QuestionType.TRUE_FALSE -> R.string.type_true_false
            QuestionType.POLL -> R.string.type_poll
        },
    )

    private fun answerName(question: SessionQuestionEntity, option: AnswerOption): String = when {
        question.type == QuestionType.TRUE_FALSE && option == AnswerOption.A -> string(R.string.answer_true)
        question.type == QuestionType.TRUE_FALSE && option == AnswerOption.B -> string(R.string.answer_false)
        else -> option.name
    }

    private fun string(id: Int) = context.getString(id)

    private inline fun writeStream(name: String, content: (java.io.OutputStream) -> Unit): File {
        val file = Sharing.exportFile(context, name)
        file.outputStream().use(content)
        return file
    }

    private inline fun writeBytes(name: String, bytes: () -> ByteArray): File {
        val file = Sharing.exportFile(context, name)
        file.writeBytes(bytes())
        return file
    }

    private companion object {
        const val LIGHT_RED = 0xFFFFE3E3.toInt()
        const val LIGHT_AMBER = 0xFFFFF0D6.toInt()
    }
}
