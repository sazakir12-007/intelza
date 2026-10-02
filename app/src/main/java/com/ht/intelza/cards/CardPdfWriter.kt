package com.ht.intelza.cards

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.ht.intelza.data.CardSize
import com.ht.intelza.data.PaperSize
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.scan.AprilTagDetector
import java.io.OutputStream

/** One printed answer card. */
data class CardSpec(
    val cardNumber: Int,
    val studentName: String? = null,
    val className: String? = null,
)

/**
 * Lays out answer cards as a printable PDF (requirements B1–B4).
 *
 * The tag fills as much of the card as possible, because its printed size decides how far
 * away it can be read. Letters sit just outside the tag's white border, each turned so it
 * reads upright when that edge is on top.
 */
class CardPdfWriter(
    private val paper: PaperSize,
    private val size: CardSize,
) {
    private val tagPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.FILL
    }
    private val letterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(85, 85, 85)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(40, 40, 40)
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(70, 70, 70)
    }
    private val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(170, 170, 170)
        style = Paint.Style.STROKE
        strokeWidth = 0.75f
        pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
    }

    fun write(cards: List<CardSpec>, out: OutputStream) {
        require(cards.isNotEmpty()) { "No cards to print" }
        val document = PdfDocument()
        try {
            val slotHeight = paper.heightPt.toFloat() / size.cardsPerPage
            cards.chunked(size.cardsPerPage).forEachIndexed { pageIndex, pageCards ->
                val info = PdfDocument.PageInfo.Builder(paper.widthPt, paper.heightPt, pageIndex + 1).create()
                val page = document.startPage(info)
                pageCards.forEachIndexed { slot, card ->
                    val area = RectF(0f, slot * slotHeight, paper.widthPt.toFloat(), (slot + 1) * slotHeight)
                    drawCard(page.canvas, card, area)
                }
                for (slot in 1 until size.cardsPerPage) {
                    val y = slot * slotHeight
                    page.canvas.drawLine(MARGIN, y, paper.widthPt - MARGIN, y, cutPaint)
                }
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private fun drawCard(canvas: Canvas, card: CardSpec, area: RectF) {
        val large = size == CardSize.LARGE
        val letterBand = if (large) 34f else 28f
        val gridSize = minOf(area.width(), area.height()) - 2 * (MARGIN + letterBand)
        val grid = RectF(
            area.centerX() - gridSize / 2,
            area.centerY() - gridSize / 2,
            area.centerX() + gridSize / 2,
            area.centerY() + gridSize / 2,
        )
        drawTag(canvas, card.cardNumber, grid)

        letterPaint.textSize = if (large) 22f else 17f
        val offset = letterBand / 2
        drawLetter(canvas, AnswerOption.A, grid.centerX(), grid.top - offset, 0f)
        drawLetter(canvas, AnswerOption.B, grid.right + offset, grid.centerY(), 90f)
        drawLetter(canvas, AnswerOption.C, grid.centerX(), grid.bottom + offset, 180f)
        drawLetter(canvas, AnswerOption.D, grid.left - offset, grid.centerY(), 270f)

        numberPaint.textSize = if (large) 20f else 15f
        val numberBaseline = area.top + MARGIN + numberPaint.textSize
        canvas.drawText("#${card.cardNumber}", area.left + MARGIN, numberBaseline, numberPaint)

        namePaint.textSize = if (large) 12f else 10f
        val maxNameWidth = grid.left - letterBand - area.left - MARGIN
        var lineBaseline = numberBaseline + namePaint.textSize + 4f
        for (line in listOfNotNull(card.studentName, card.className).filter { it.isNotBlank() }) {
            canvas.drawText(ellipsize(line, maxNameWidth), area.left + MARGIN, lineBaseline, namePaint)
            lineBaseline += namePaint.textSize + 3f
        }
    }

    /** Draws the tag's black cells as one path so adjacent cells print without seams. */
    private fun drawTag(canvas: Canvas, id: Int, grid: RectF) {
        val cells = AprilTagDetector.tagCells(id)
        val cell = grid.width() / cells.size
        val path = Path()
        for (y in cells.indices) {
            for (x in cells[y].indices) {
                if (cells[y][x]) {
                    path.addRect(
                        grid.left + x * cell,
                        grid.top + y * cell,
                        grid.left + (x + 1) * cell,
                        grid.top + (y + 1) * cell,
                        Path.Direction.CW,
                    )
                }
            }
        }
        canvas.drawPath(path, tagPaint)
    }

    private fun drawLetter(canvas: Canvas, option: AnswerOption, x: Float, y: Float, degrees: Float) {
        canvas.save()
        canvas.rotate(degrees, x, y)
        val metrics = letterPaint.fontMetrics
        canvas.drawText(option.name, x, y - (metrics.ascent + metrics.descent) / 2, letterPaint)
        canvas.restore()
    }

    private fun ellipsize(text: String, maxWidth: Float): String {
        if (maxWidth <= 0f || namePaint.measureText(text) <= maxWidth) return text
        var end = text.length
        while (end > 1 && namePaint.measureText(text, 0, end) + namePaint.measureText("…") > maxWidth) end--
        return text.take(end).trimEnd() + "…"
    }

    private companion object {
        const val MARGIN = 20f
    }
}
