package com.ht.intelza.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.ht.intelza.R

/** The INTELZA wordmark with its tagline, "Understanding Beyond Answers". */
@Composable
fun BrandLockup(
    modifier: Modifier = Modifier,
    wordmarkSize: TextUnit = 22.sp,
    taglineSize: TextUnit = 12.sp,
    wordmarkColor: Color = MaterialTheme.colorScheme.primary,
    taglineColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
) {
    Column(modifier, horizontalAlignment = horizontalAlignment) {
        Text(
            stringResource(R.string.app_wordmark),
            color = wordmarkColor,
            fontSize = wordmarkSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.sp,
        )
        Text(
            stringResource(R.string.app_tagline),
            color = taglineColor,
            fontSize = taglineSize,
            letterSpacing = 0.5.sp,
        )
    }
}
