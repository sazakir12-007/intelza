package com.ht.intelza.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

/**
 * A small flowing-layout writer for A4 portrait PDF reports: headings, paragraphs and
 * simple tables, with automatic page breaks.
 */
class PdfReportWriter(private val footer: String) {
    private val document = PdfDocument()
    private var page: PdfDocument.Page? = null
    private var pageNumber = 0
    private var y = 0f

    private val titlePaint = textPaint(20f, bold = true)
    private val headingPaint = textPaint(13f, bold = true, color = Color.rgb(63, 81, 196))
    private val bodyPaint = textPaint(10f)
    private val boldPaint = textPaint(10f, bold = true)
    private val smallPaint = textPaint(8f, color = Color.GRAY)
    private val subtitlePaint = textPaint(10f, color = Color.DKGRAY)
    private val linePaint = Paint().apply {
        color = Color.rgb(220, 220, 225)
        strokeWidth = 0.6f
    }
    private val fillPaint = Paint().apply { style = Paint.Style.FILL }

    private val canvas: Canvas get() = (page ?: newPage()).canvas

    fun title(text: String) {
        ensureSpace(30f)
        canvas.drawText(text, MARGIN, y + 18f, titlePaint)
        y += 30f
    }

    fun subtitle(text: String) = paragraph(text, subtitlePaint)

    fun heading(text: String) {
        ensureSpace(34f)
        y += 10f
        canvas.drawText(text, MARGIN, y + 13f, headingPaint)
        y += 22f
    }

    fun paragraph(text: String, paint: Paint = bodyPaint) {
        for (line in wrap(text, paint, CONTENT_WIDTH)) {
            ensureSpace(LINE)
            canvas.drawText(line, MARGIN, y + paint.textSize, paint)
            y += LINE
        }
        y += 4f
    }

    /** A row of large figures, e.g. averages. */
    fun stats(items: List<Pair<String, String>>) {
        ensureSpace(46f)
        val width = CONTENT_WIDTH / items.size.coerceAtLeast(1)
        val valuePaint = textPaint(18f, bold = true)
        items.forEachIndexed { index, (label, value) ->
            val x = MARGIN + index * width
            canvas.drawText(value, x, y + 20f, valuePaint)
            canvas.drawText(label, x, y + 34f, smallPaint)
        }
        y += 46f
    }

    /**
     * Draws a table. [weights] split the content width between columns; rows that don't
     * fit start a new page with the header repeated.
     */
    fun table(header: List<String>, rows: List<List<String>>, weights: List<Float>, highlight: (Int) -> Int? = { null }) {
        val total = weights.sum()
        val widths = weights.map { it / total * CONTENT_WIDTH }
        fun drawRow(cells: List<String>, paint: Paint, background: Int?) {
            ensureSpace(ROW)
            if (background != null) {
                fillPaint.color = background
                canvas.drawRect(MARGIN, y, MARGIN + CONTENT_WIDTH, y + ROW, fillPaint)
            }
            var x = MARGIN
            cells.forEachIndexed { i, cell ->
                val width = widths.getOrElse(i) { 0f }
                canvas.drawText(ellipsize(cell, paint, width - 6f), x + 3f, y + 12f, paint)
                x += width
            }
            y += ROW
            canvas.drawLine(MARGIN, y, MARGIN + CONTENT_WIDTH, y, linePaint)
        }
        ensureSpace(ROW * 2)
        drawRow(header, boldPaint, Color.rgb(239, 237, 244))
        rows.forEachIndexed { index, row ->
            if (y + ROW > PAGE_HEIGHT - BOTTOM) {
                newPage()
                drawRow(header, boldPaint, Color.rgb(239, 237, 244))
            }
            drawRow(row, bodyPaint, highlight(index))
        }
        y += 6f
    }

    fun write(out: OutputStream) {
        finishPage()
        document.writeTo(out)
        document.close()
    }

    private fun ensureSpace(height: Float) {
        if (page == null || y + height > PAGE_HEIGHT - BOTTOM) newPage()
    }

    private fun newPage(): PdfDocument.Page {
        finishPage()
        pageNumber++
        val created = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        page = created
        y = TOP
        return created
    }

    private fun finishPage() {
        val current = page ?: return
        current.canvas.drawText("$footer · $pageNumber", MARGIN, PAGE_HEIGHT - 24f, smallPaint)
        document.finishPage(current)
        page = null
    }

    private fun wrap(text: String, paint: Paint, width: Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraph in text.split('\n')) {
            var line = ""
            for (word in paragraph.split(' ')) {
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) <= width || line.isEmpty()) {
                    line = candidate
                } else {
                    lines += line
                    line = word
                }
            }
            lines += line
        }
        return lines
    }

    private fun ellipsize(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var end = text.length
        while (end > 0 && paint.measureText(text, 0, end) + paint.measureText("…") > width) end--
        return text.take(end) + "…"
    }

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.rgb(27, 27, 33)) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = Typeface.create(Typeface.DEFAULT, if (bold) Typeface.BOLD else Typeface.NORMAL)
        }

    private companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN = 40f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        const val TOP = 40f
        const val BOTTOM = 48f
        const val LINE = 14f
        const val ROW = 17f
    }
}
