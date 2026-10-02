package com.ht.intelza.data.csv

/** Minimal RFC 4180 CSV reading and writing (quotes, escaped quotes, line breaks in cells). */
object Csv {

    /**
     * Parses [text] into rows of cells. The delimiter (comma, semicolon or tab) is guessed
     * from the first line, since spreadsheet apps in some regions export semicolons.
     */
    fun parse(text: String): List<List<String>> {
        val content = text.removePrefix("﻿")
        val delimiter = guessDelimiter(content.lineSequence().firstOrNull().orEmpty())
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < content.length) {
            val c = content[i]
            when {
                inQuotes && c == '"' && content.getOrNull(i + 1) == '"' -> {
                    cell.append('"')
                    i++
                }
                c == '"' && (inQuotes || cell.isEmpty()) -> inQuotes = !inQuotes
                !inQuotes && c == delimiter -> {
                    row.add(cell.toString())
                    cell.clear()
                }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && content.getOrNull(i + 1) == '\n') i++
                    row.add(cell.toString())
                    cell.clear()
                    rows.add(row)
                    row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows.map { cells -> cells.map { it.trim() } }.filter { cells -> cells.any { it.isNotEmpty() } }
    }

    fun write(rows: List<List<Any?>>): String = buildString {
        for (row in rows) {
            row.joinTo(this, separator = ",") { escape(it?.toString().orEmpty()) }
            append("\r\n")
        }
    }

    private fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' } || value.startsWith(" ")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }

    private fun guessDelimiter(firstLine: String): Char =
        listOf(',', ';', '\t').maxBy { d -> firstLine.count { it == d } }
}
