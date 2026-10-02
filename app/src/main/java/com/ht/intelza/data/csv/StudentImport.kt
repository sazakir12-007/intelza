package com.ht.intelza.data.csv

/** A student read from an imported file, before card numbers are checked. */
data class StudentImportRow(
    val name: String,
    val rollNumber: String,
    val cardNumber: Int?,
)

/**
 * Reads a student list exported from a spreadsheet.
 *
 * With a header row, columns are found by name ("name", "roll", "card"). Without one, the
 * columns are taken as name, roll number, card number. Only the name is required.
 */
object StudentImport {

    fun parse(text: String): List<StudentImportRow> {
        val rows = Csv.parse(text)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.lowercase() }
        val hasHeader = header.any { NAME_HEADER.containsMatchIn(it) }
        val nameColumn = if (hasHeader) header.indexOfFirst { NAME_HEADER.containsMatchIn(it) } else 0
        val rollColumn = if (hasHeader) header.indexOfFirst { it.contains("roll") || it in ROLL_HEADERS } else 1
        val cardColumn = if (hasHeader) header.indexOfFirst { it.contains("card") } else 2
        return rows.drop(if (hasHeader) 1 else 0).mapNotNull { cells ->
            val name = cells.getOrNull(nameColumn)?.trim().orEmpty()
            if (name.isEmpty()) return@mapNotNull null
            StudentImportRow(
                name = name,
                rollNumber = cells.getOrNull(rollColumn)?.trim().orEmpty(),
                cardNumber = cells.getOrNull(cardColumn)?.trim()?.toIntOrNull(),
            )
        }
    }

    private val NAME_HEADER = Regex("""\bnames?\b|^student$""")
    private val ROLL_HEADERS = setOf("no", "no.", "number", "#", "s.no", "sr. no.")
}
