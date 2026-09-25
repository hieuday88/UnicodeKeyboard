// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.unicode

import android.content.SharedPreferences
import androidx.core.content.edit

/** Unicode mode: replaces letters and digits with look-alike Unicode characters. Data ported from CMkey UnicodeEngine. */
object UnicodeEngine {
    const val PREF_UNICODE_MODE = "unicode_mode"
    const val PREF_CUSTOM_OVERRIDES = "unicode_custom_overrides"

    private val lookAlike = mapOf(
        '2' to "ᒿ", '3' to "ვ", '4' to "Ꮞ",
        'a' to "ɑ", 'e' to "ϵ", 'i' to "ɩ", 'o' to "σ", 'u' to "υ", 'y' to "γ",
        'b' to "ხ", 'c' to "с", 'd' to "ᑯ", 'đ' to "ᴆ", 'f' to "ғ", 'g' to "ԍ",
        'h' to "ҥ", 'j' to "ј", 'k' to "κ", 'l' to "ℓ", 'm' to "ʍ", 'n' to "ͷ",
        'p' to "ρ", 'q' to "ԛ", 'r' to "ʀ", 's' to "ຣ", 't' to "τ", 'v' to "ѵ",
        'w' to "ω", 'x' to "х", 'z' to "ⴭ",
    )

    private val upper = mapOf(
        'A' to "ᗅ", 'B' to "β", 'C' to "С", 'D' to "ᗪ", 'Đ' to "Ð", 'E' to "€",
        'F' to "Ғ", 'G' to "Ԍ", 'H' to "Ң", 'I' to "l", 'J' to "Ꭻ", 'K' to "₭",
        'L' to "Լ", 'M' to "Ϻ", 'N' to "Ν", 'O' to "ϴ", 'P' to "ᑭ", 'Q' to "Ԛ",
        'R' to "Ʀ", 'S' to "ჽ", 'T' to "Ͳ", 'U' to "Ս", 'Ư' to "Մ", 'V' to "Ѵ",
        'W' to "Ԝ", 'X' to "Х", 'Y' to "ϒ", 'Z' to "Ζ",
    )

    private val digit = mapOf(
        '0' to "θ", '1' to "1", '2' to "２", '3' to "ვ", '4' to "Ꮞ",
        '5' to "Ƽ", '6' to "б", '7' to "⁊", '8' to "Ȣ", '9' to "୨",
    )

    /** Preset "Default 2": Latin small capitals for lowercase ASCII letters. */
    val default2Overrides: Map<Char, String> = mapOf(
        'a' to "ᴀ", 'b' to "ʙ", 'c' to "ᴄ", 'd' to "ᴅ", 'e' to "ᴇ", 'f' to "ꜰ", 'g' to "ɢ",
        'h' to "ʜ", 'i' to "ɪ", 'j' to "ᴊ", 'k' to "ᴋ", 'l' to "ʟ", 'm' to "ᴍ", 'n' to "ɴ",
        'o' to "ᴏ", 'p' to "ᴘ", 'q' to "ᨾ", 'r' to "ʀ", 's' to "ꜱ", 't' to "ᴛ", 'u' to "ᴜ",
        'v' to "ᴠ", 'w' to "ᴡ", 'x' to "x", 'y' to "ʏ", 'z' to "ᴢ",
    )

    @Volatile var unicodeActive = false
        private set
    @Volatile private var customOverrides: Map<Char, String> = emptyMap()

    /** Reloads Unicode mode state and user overrides from preferences. */
    fun load(prefs: SharedPreferences) {
        unicodeActive = prefs.getBoolean(PREF_UNICODE_MODE, false)
        customOverrides = decodeOverrides(prefs.getString(PREF_CUSTOM_OVERRIDES, "") ?: "")
    }

    /** Toggles Unicode mode and persists it. */
    fun toggleUnicode(prefs: SharedPreferences) {
        val enabled = !unicodeActive
        unicodeActive = enabled
        prefs.edit { putBoolean(PREF_UNICODE_MODE, enabled) }
    }

    fun getCustomOverrides(): Map<Char, String> = customOverrides

    /** Replaces all user overrides and persists them. */
    fun setCustomOverrides(prefs: SharedPreferences, overrides: Map<Char, String>) {
        customOverrides = overrides.filterValues { it.isNotEmpty() }
        prefs.edit { putString(PREF_CUSTOM_OVERRIDES, encodeOverrides(customOverrides)) }
    }

    /** Returns the replacement for [ch], or null if it stays unchanged. */
    fun transform(ch: Char): String? = customOverrides[ch] ?: transformDefault(ch)

    /** Returns the built-in replacement for [ch], ignoring user overrides. */
    fun transformDefault(ch: Char): String? {
        if (ch.isUpperCase()) upper[ch]?.let { return it }
        val lower = ch.lowercaseChar()
        val replacement = lookAlike[lower] ?: (if (lower.isDigit()) digit[lower] else null) ?: return null
        val result = if (ch.isUpperCase()) replacement.uppercase() else replacement
        return if (result == ch.toString()) null else result
    }

    /** Applies the Unicode filter to [input] when Unicode mode is on, otherwise returns it unchanged. */
    fun filterIfActive(input: CharSequence): CharSequence {
        if (!unicodeActive || input.isEmpty()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) sb.append(transform(ch) ?: ch.toString())
        return sb
    }

    private fun encodeOverrides(map: Map<Char, String>) =
        map.entries.joinToString("\n") { "${it.key.code}\t${it.value}" }

    private fun decodeOverrides(s: String): Map<Char, String> = s.lineSequence().mapNotNull { line ->
        val parts = line.split('\t', limit = 2)
        val code = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
        val value = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        if (code !in 0..0xFFFF) null else code.toChar() to value
    }.toMap()
}
