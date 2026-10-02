package com.ht.intelza.ui.cards

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ht.intelza.R
import com.ht.intelza.cards.CardPdfWriter
import com.ht.intelza.cards.CardSpec
import com.ht.intelza.data.CardSize
import com.ht.intelza.data.ClassRepository
import com.ht.intelza.data.PaperSize
import com.ht.intelza.data.SettingsRepository
import com.ht.intelza.domain.CardNumbers
import com.ht.intelza.export.Sharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import com.ht.intelza.data.db.StudentEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class CardOptions(
    /** True to print the class's students; false for a plain numbered set. */
    val forClass: Boolean,
    val includeNames: Boolean,
    val rangeFrom: Int,
    val rangeTo: Int,
    val paper: PaperSize,
    val size: CardSize,
)

data class CardPreview(
    val file: File,
    val firstPage: Bitmap,
    val pageCount: Int,
    val cardCount: Int,
)

class PrintCardsViewModel(
    private val app: Application,
    private val classes: ClassRepository,
    private val settings: SettingsRepository,
    savedState: SavedStateHandle,
) : ViewModel() {
    val classId: Long? = savedState.get<Long>("classId")?.takeIf { it > 0 }

    private val _className = MutableStateFlow<String?>(null)
    val className: StateFlow<String?> = _className.asStateFlow()

    private val _options = MutableStateFlow<CardOptions?>(null)
    val options: StateFlow<CardOptions?> = _options.asStateFlow()

    private val _preview = MutableStateFlow<CardPreview?>(null)
    val preview: StateFlow<CardPreview?> = _preview.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val current = settings.current()
            val schoolClass = classId?.let { classes.getClass(it) }
            _className.value = schoolClass?.name
            val studentCount = classId?.let { classes.getStudents(it).size } ?: 0
            _options.value = CardOptions(
                forClass = schoolClass != null,
                includeNames = current.printNamesOnCards,
                rangeFrom = 1,
                rangeTo = studentCount.takeIf { it > 0 } ?: DEFAULT_SET_SIZE,
                paper = current.paperSize,
                size = current.cardSize,
            )
        }
        viewModelScope.launch { regenerateOnChange() }
    }

    fun update(transform: (CardOptions) -> CardOptions) {
        val updated = _options.value?.let(transform) ?: return
        _options.value = updated
        viewModelScope.launch {
            settings.update {
                it.copy(paperSize = updated.paper, cardSize = updated.size, printNamesOnCards = updated.includeNames)
            }
        }
    }

    fun print() {
        val preview = _preview.value ?: return
        Sharing.printPdf(app, preview.file, preview.file.nameWithoutExtension)
    }

    fun share() {
        val preview = _preview.value ?: return
        Sharing.share(app, preview.file, "application/pdf", app.getString(R.string.share_cards))
    }

    @OptIn(FlowPreview::class)
    private suspend fun regenerateOnChange() {
        val students: Flow<List<StudentEntity>> = classId?.let { classes.observeStudents(it) } ?: flowOf(emptyList())
        combine(_options.debounce(250), students) { options, list -> options to list }.collectLatest { (options, studentList) ->
            if (options == null) return@collectLatest
            val specs = if (options.forClass) {
                studentList.filter { it.cardNumber != null }.sortedBy { it.cardNumber }.map {
                    CardSpec(
                        cardNumber = it.cardNumber!!,
                        studentName = it.name.takeIf { options.includeNames },
                        className = _className.value.takeIf { options.includeNames },
                    )
                }
            } else {
                val from = options.rangeFrom.coerceIn(CardNumbers.MIN, CardNumbers.MAX)
                val to = options.rangeTo.coerceIn(from, minOf(CardNumbers.MAX, from + MAX_SET_SIZE - 1))
                (from..to).map { CardSpec(it) }
            }
            if (specs.isEmpty()) {
                _preview.value = null
                _error.value = app.getString(R.string.no_cards_to_print)
                return@collectLatest
            }
            _error.value = null
            _preview.value = withContext(Dispatchers.Default) { render(specs, options) }
        }
    }

    private fun render(specs: List<CardSpec>, options: CardOptions): CardPreview {
        val name = _className.value?.takeIf { options.forClass }
            ?.let { app.getString(R.string.cards_file_name_class, it) }
            ?: app.getString(R.string.cards_file_name_set, specs.first().cardNumber, specs.last().cardNumber)
        val file = Sharing.exportFile(app, "$name.pdf")
        file.outputStream().use { CardPdfWriter(options.paper, options.size).write(specs, it) }
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                renderer.openPage(0).use { page ->
                    val width = PREVIEW_WIDTH
                    val height = width * page.height / page.width
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return CardPreview(file, bitmap, renderer.pageCount, specs.size)
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_SET_SIZE = 40
        const val MAX_SET_SIZE = 200
        const val PREVIEW_WIDTH = 900
    }
}
