package com.ht.intelza.ui.questions

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.material3.MaterialTheme
import com.ht.intelza.R
import com.ht.intelza.domain.AnswerOption
import com.ht.intelza.domain.QuestionType
import com.ht.intelza.ui.common.appContainer

@Composable
fun questionTypeLabel(type: QuestionType): String = stringResource(
    when (type) {
        QuestionType.MULTIPLE_CHOICE -> R.string.type_multiple_choice
        QuestionType.TRUE_FALSE -> R.string.type_true_false
        QuestionType.POLL -> R.string.type_poll
    },
)

/** The text shown for an answer option: True/False for those questions, otherwise the typed text. */
@Composable
fun optionText(type: QuestionType, option: AnswerOption, typed: String): String = when {
    type == QuestionType.TRUE_FALSE && option == AnswerOption.A -> stringResource(R.string.answer_true)
    type == QuestionType.TRUE_FALSE && option == AnswerOption.B -> stringResource(R.string.answer_false)
    else -> typed
}

/** Short description such as "Multiple choice · Answer B". */
@Composable
fun questionSummary(type: QuestionType, optionCount: Int, correct: AnswerOption?, optionA: String): String {
    val parts = mutableListOf(questionTypeLabel(type))
    when {
        type == QuestionType.POLL -> parts += stringResource(R.string.option_count, type.optionCount(optionCount))
        correct != null -> parts += stringResource(R.string.answer_value, optionText(type, correct, correct.name))
        else -> parts += stringResource(R.string.no_correct_answer_set)
    }
    return parts.joinToString(" · ")
}

fun optionTexts(a: String, b: String, c: String, d: String): Map<AnswerOption, String> =
    mapOf(AnswerOption.A to a, AnswerOption.B to b, AnswerOption.C to c, AnswerOption.D to d)

/** Shows a stored question picture, loading it off the main thread. */
@Composable
fun StoredImage(
    name: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    maxSize: Int = 1200,
    contentDescription: String? = null,
) {
    val images = appContainer().images
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name, maxSize) {
        value = images.load(name, maxSize)?.asImageBitmap()
    }
    val image = bitmap
    if (image != null) {
        Image(image, contentDescription, modifier, contentScale = contentScale)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
