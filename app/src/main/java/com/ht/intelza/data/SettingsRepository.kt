package com.ht.intelza.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class PaperSize(val widthPt: Int, val heightPt: Int) {
    A4(595, 842),
    LETTER(612, 792),
}

enum class CardSize(val cardsPerPage: Int) {
    /** Two cards per page. */
    STANDARD(2),

    /** One card per page, for big rooms. */
    LARGE(1),
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Accent colours the teacher can choose; DYNAMIC follows the wallpaper (Android 12+). */
enum class ThemePalette { INDIGO, TEAL, PURPLE, ORANGE, DYNAMIC }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val themePalette: ThemePalette = ThemePalette.INDIGO,
    val needsHelpThreshold: Int = 50,
    val reteachThreshold: Int = 60,
    val paperSize: PaperSize = PaperSize.A4,
    val cardSize: CardSize = CardSize.STANDARD,
    val printNamesOnCards: Boolean = true,
    val scanSound: Boolean = true,
    val scanVibration: Boolean = true,
    val showNamesWhileScanning: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(context: Context) {
    private val store = context.applicationContext.settingsStore

    val settings: Flow<AppSettings> = store.data.map { it.toSettings() }

    suspend fun current(): AppSettings = settings.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val updated = transform(prefs.toSettings())
            prefs[THEME_MODE] = updated.themeMode.name
            prefs[THEME_PALETTE] = updated.themePalette.name
            prefs[NEEDS_HELP] = updated.needsHelpThreshold.coerceIn(0, 100)
            prefs[RETEACH] = updated.reteachThreshold.coerceIn(0, 100)
            prefs[PAPER] = updated.paperSize.name
            prefs[CARD_SIZE] = updated.cardSize.name
            prefs[PRINT_NAMES] = updated.printNamesOnCards
            prefs[SCAN_SOUND] = updated.scanSound
            prefs[SCAN_VIBRATION] = updated.scanVibration
            prefs[SHOW_NAMES] = updated.showNamesWhileScanning
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            themeMode = this[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.themeMode,
            themePalette = this[THEME_PALETTE]?.let { runCatching { ThemePalette.valueOf(it) }.getOrNull() }
                ?: defaults.themePalette,
            needsHelpThreshold = this[NEEDS_HELP] ?: defaults.needsHelpThreshold,
            reteachThreshold = this[RETEACH] ?: defaults.reteachThreshold,
            paperSize = this[PAPER]?.let { runCatching { PaperSize.valueOf(it) }.getOrNull() } ?: defaults.paperSize,
            cardSize = this[CARD_SIZE]?.let { runCatching { CardSize.valueOf(it) }.getOrNull() } ?: defaults.cardSize,
            printNamesOnCards = this[PRINT_NAMES] ?: defaults.printNamesOnCards,
            scanSound = this[SCAN_SOUND] ?: defaults.scanSound,
            scanVibration = this[SCAN_VIBRATION] ?: defaults.scanVibration,
            showNamesWhileScanning = this[SHOW_NAMES] ?: defaults.showNamesWhileScanning,
        )
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val NEEDS_HELP = intPreferencesKey("needs_help_threshold")
        val RETEACH = intPreferencesKey("reteach_threshold")
        val PAPER = stringPreferencesKey("paper_size")
        val CARD_SIZE = stringPreferencesKey("card_size")
        val PRINT_NAMES = booleanPreferencesKey("print_names_on_cards")
        val SCAN_SOUND = booleanPreferencesKey("scan_sound")
        val SCAN_VIBRATION = booleanPreferencesKey("scan_vibration")
        val SHOW_NAMES = booleanPreferencesKey("show_names_while_scanning")
    }
}
