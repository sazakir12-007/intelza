package com.ht.intelza.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.ht.intelza.data.ThemePalette
import com.ht.intelza.domain.AnswerOption

internal val LightColors = lightColorScheme(
    primary = Color(0xFF3F51C4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDFE0FF),
    onPrimaryContainer = Color(0xFF000D60),
    secondary = Color(0xFF006B5F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF9EF2E2),
    onSecondaryContainer = Color(0xFF00201C),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2C1600),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFBF8FF),
    onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF),
    onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    outline = Color(0xFF777680),
    outlineVariant = Color(0xFFC7C5D0),
    inverseSurface = Color(0xFF303036),
    inverseOnSurface = Color(0xFFF2EFF7),
    inversePrimary = Color(0xFFBCC2FF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4),
    surfaceContainerHigh = Color(0xFFE9E7EF),
    surfaceContainerHighest = Color(0xFFE4E1E9),
)

internal val DarkColors = darkColorScheme(
    primary = Color(0xFFBCC2FF),
    onPrimary = Color(0xFF14248F),
    primaryContainer = Color(0xFF2B3AAA),
    onPrimaryContainer = Color(0xFFDFE0FF),
    secondary = Color(0xFF82D5C6),
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF005047),
    onSecondaryContainer = Color(0xFF9EF2E2),
    tertiary = Color(0xFFFFB86F),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = Color(0xFF693C00),
    onTertiaryContainer = Color(0xFFFFDCBE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF131318),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF131318),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    outline = Color(0xFF91909A),
    outlineVariant = Color(0xFF46464F),
    inverseSurface = Color(0xFFE4E1E9),
    inverseOnSurface = Color(0xFF303036),
    inversePrimary = Color(0xFF3F51C4),
    surfaceContainerLowest = Color(0xFF0E0E13),
    surfaceContainerLow = Color(0xFF1B1B21),
    surfaceContainer = Color(0xFF1F1F25),
    surfaceContainerHigh = Color(0xFF2A292F),
    surfaceContainerHighest = Color(0xFF35343A),
)

// Other accent palettes share the neutral surfaces of the default (indigo) scheme.

internal val TealLight = LightColors.copy(
    primary = Color(0xFF006A60),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF74F8E5),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4A635F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E2),
    onSecondaryContainer = Color(0xFF05201C),
    tertiary = Color(0xFF456179),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCE5FF),
    onTertiaryContainer = Color(0xFF001E31),
    inversePrimary = Color(0xFF53DBC9),
)

internal val TealDark = DarkColors.copy(
    primary = Color(0xFF53DBC9),
    onPrimary = Color(0xFF003731),
    primaryContainer = Color(0xFF005048),
    onPrimaryContainer = Color(0xFF74F8E5),
    secondary = Color(0xFFB1CCC6),
    onSecondary = Color(0xFF1C3531),
    secondaryContainer = Color(0xFF334B47),
    onSecondaryContainer = Color(0xFFCCE8E2),
    tertiary = Color(0xFFADCAE6),
    onTertiary = Color(0xFF153349),
    tertiaryContainer = Color(0xFF2D4960),
    onTertiaryContainer = Color(0xFFCCE5FF),
    inversePrimary = Color(0xFF006A60),
)

internal val PurpleLight = LightColors.copy(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    inversePrimary = Color(0xFFD0BCFF),
)

internal val PurpleDark = DarkColors.copy(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    inversePrimary = Color(0xFF6750A4),
)

internal val OrangeLight = LightColors.copy(
    primary = Color(0xFFA04100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCC),
    onPrimaryContainer = Color(0xFF351000),
    secondary = Color(0xFF76574A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDBCC),
    onSecondaryContainer = Color(0xFF2C160C),
    tertiary = Color(0xFF665F31),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFEEE4A9),
    onTertiaryContainer = Color(0xFF201C00),
    inversePrimary = Color(0xFFFFB693),
)

internal val OrangeDark = DarkColors.copy(
    primary = Color(0xFFFFB693),
    onPrimary = Color(0xFF561F00),
    primaryContainer = Color(0xFF7A3000),
    onPrimaryContainer = Color(0xFFFFDBCC),
    secondary = Color(0xFFE6BEAD),
    onSecondary = Color(0xFF442A1F),
    secondaryContainer = Color(0xFF5D4034),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFD1C88F),
    onTertiary = Color(0xFF363107),
    tertiaryContainer = Color(0xFF4D481C),
    onTertiaryContainer = Color(0xFFEEE4A9),
    inversePrimary = Color(0xFFA04100),
)

/** The swatch shown for each palette in settings. */
val PaletteSwatches = mapOf(
    ThemePalette.INDIGO to Color(0xFF3F51C4),
    ThemePalette.TEAL to Color(0xFF006A60),
    ThemePalette.PURPLE to Color(0xFF6750A4),
    ThemePalette.ORANGE to Color(0xFFA04100),
)

/** Fixed colours used for answer letters in charts and on the scanner. */
object AnswerColors {
    val A = Color(0xFF4263EB)
    val B = Color(0xFF0CA678)
    val C = Color(0xFFF59F00)
    val D = Color(0xFFD6336C)

    fun of(option: AnswerOption): Color = when (option) {
        AnswerOption.A -> A
        AnswerOption.B -> B
        AnswerOption.C -> C
        AnswerOption.D -> D
    }
}

/** Colours for right/wrong feedback, readable in both light and dark themes. */
object ResultColors {
    val correct = Color(0xFF2F9E44)
    val incorrect = Color(0xFFE03131)
    val noAnswer = Color(0xFF868E96)
    val warning = Color(0xFFF08C00)
}
