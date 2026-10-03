package com.ht.intelza.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ht.intelza.data.ThemePalette

/** Slightly smaller type than Material's defaults, for a compact layout. */
private val CompactTypography = Typography().run {
    copy(
        displaySmall = displaySmall.copy(fontSize = 30.sp, lineHeight = 36.sp),
        headlineMedium = headlineMedium.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodyLarge = bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp),
        bodyMedium = bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
        bodySmall = bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = labelLarge.copy(fontSize = 13.sp, lineHeight = 18.sp),
        labelMedium = labelMedium.copy(fontSize = 11.sp, lineHeight = 15.sp),
        labelSmall = labelSmall.copy(fontSize = 10.sp, lineHeight = 14.sp),
    )
}

@Composable
fun IntelzaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    palette: ThemePalette = ThemePalette.INDIGO,
    content: @Composable () -> Unit,
) {
    val colors = if (palette == ThemePalette.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        colorScheme(palette, darkTheme)
    }
    MaterialTheme(colorScheme = colors, typography = CompactTypography) {
        // Compact: smaller minimum touch area around icons, switches and chips.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 40.dp, content = content)
    }
}

fun colorScheme(palette: ThemePalette, dark: Boolean): ColorScheme = when (palette) {
    ThemePalette.INDIGO, ThemePalette.DYNAMIC -> if (dark) DarkColors else LightColors
    ThemePalette.TEAL -> if (dark) TealDark else TealLight
    ThemePalette.PURPLE -> if (dark) PurpleDark else PurpleLight
    ThemePalette.ORANGE -> if (dark) OrangeDark else OrangeLight
}
