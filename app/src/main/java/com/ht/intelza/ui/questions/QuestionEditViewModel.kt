package com.ht.intelza.ui.questions

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ht.intelza.data.ImageStore
import com.ht.intelza.data.QuestionRepository
import com.ht.intelza.data.db.QuestionEntity
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuestionForm(
    val type: QuestionType = QuestionType.MULTIPLE_CHOICE,
    val text: String = "",
    val optionCount: Int = 4,
    val options: List<String> = List(4) { "" },
    val correct: AnswerOption? = null,
    val imageName: String? = null,
    val loaded: Boolean = false,
    val importingImage: Boolean = false,
    val imageError: Boolean = false,
) {
    val visibleOptions: List<AnswerOption> get() = AnswerOption.firstN(type.optionCount(optionCount))

    val hasContent: Boolean get() = text.isNotBlank() || imageName != null

    val hasValidAnswer: Boolean
        get() = when (type) {
            QuestionType.POLL -> true
            else -> correct != null && correct in visibleOptions
        }

    val canSave: Boolean get() = loaded && hasContent && hasValidAnswer && !importingImage
}

class QuestionEditViewModel(
    private val questions: QuestionRepository,
    private val images: ImageStore,
    savedState: SavedStateHandle,
) : ViewModel() {
    val topicId: Long = checkNotNull(savedState["topicId"])
    private var questionId: Long? = savedState.get<Long>("questionId")?.takeIf { it > 0 }
    private var position = 0

    val isNew: Boolean get() = questionId == null

    private val _form = MutableStateFlow(QuestionForm(loaded = questionId == null))
    val form: StateFlow<QuestionForm> = _form.asStateFlow()

    init {
        questionId?.let { id ->
            viewModelScope.launch {
                val question = questions.getQuestion(id) ?: return@launch
                position = question.position
                _form.value = QuestionForm(
                    type = question.type,
                    text = question.text,
                    optionCount = question.optionCount,
                    options = listOf(question.optionA, question.optionB, question.optionC, question.optionD),
                    correct = question.correct,
                    imageName = question.imageName,
                    loaded = true,
                )
            }
        }
    }

    fun setType(type: QuestionType) = _form.update { form ->
        when (type) {
            QuestionType.TRUE_FALSE -> form.copy(
                type = type,
                correct = form.correct?.takeIf { it == AnswerOption.A || it == AnswerOption.B },
            )
            QuestionType.POLL -> form.copy(type = type, correct = null)
            QuestionType.MULTIPLE_CHOICE -> form.copy(type = type)
        }
    }

    fun setText(text: String) = _form.update { it.copy(text = text) }

    fun setOptionCount(count: Int) = _form.update { form ->
        val visible = AnswerOption.firstN(count)
        form.copy(optionCount = count, correct = form.correct?.takeIf { it in visible })
    }

    fun setOption(option: AnswerOption, text: String) = _form.update { form ->
        form.copy(options = form.options.toMutableList().also { it[option.ordinal] = text })
    }

    fun setCorrect(option: AnswerOption) = _form.update { it.copy(correct = option) }

    fun importImage(uri: Uri) {
        _form.update { it.copy(importingImage = true, imageError = false) }
        viewModelScope.launch {
            val name = runCatching { images.import(uri) }
                .onFailure { Log.w("QuestionEdit", "Could not import picture", it) }
                .getOrNull()
            _form.update {
                it.copy(imageName = name ?: it.imageName, importingImage = false, imageError = name == null)
            }
        }
    }

    fun removeImage() = _form.update { it.copy(imageName = null) }

    fun newCaptureUri(): Uri = images.newCaptureUri()

    /** Saves the question; with [addAnother] the form is cleared for the next one. */
    fun save(addAnother: Boolean, onSaved: () -> Unit) {
        val form = _form.value
        if (!form.canSave) return
        viewModelScope.launch {
            val type = form.type
            val count = type.optionCount(form.optionCount)
            val options = if (type == QuestionType.TRUE_FALSE) {
                listOf(TRUE_TEXT, FALSE_TEXT, "", "")
            } else {
                form.options.mapIndexed { index, text -> if (index < count) text.trim() else "" }
            }
            questions.saveQuestion(
                QuestionEntity(
                    id = questionId ?: 0,
                    topicId = topicId,
                    position = position,
                    type = type,
                    text = form.text.trim(),
                    optionCount = count,
                    optionA = options[0],
                    optionB = options[1],
                    optionC = options[2],
                    optionD = options[3],
                    correct = if (type.isScored) form.correct else null,
                    imageName = form.imageName,
                ),
            )
            if (addAnother) {
                questionId = null
                _form.value = QuestionForm(type = type, optionCount = form.optionCount, loaded = true)
            }
            onSaved()
        }
    }

    private companion object {
        const val TRUE_TEXT = "True"
        const val FALSE_TEXT = "False"
    }
}
