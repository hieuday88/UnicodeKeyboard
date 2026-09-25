// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.unicode

import org.junit.Assert.assertEquals
import org.junit.Test

class TelexEngineTest {
    @Test fun convertsCommonWords() {
        val cases = mapOf(
            "tieengs" to "tiếng", "vieetj" to "việt", "ddaay" to "đây", "dduwowcj" to "được",
            "nguwowif" to "người", "Tieengs" to "Tiếng", "VIEETJ" to "VIỆT", "hoaf" to "hoà",
            "thuyr" to "thuỷ", "quas" to "quá", "gias" to "giá", "khoong" to "không",
            "ass" to "as", "caaa" to "caa", "truwowngf" to "trường", "tr" to "tr",
        )
        TelexEngine.oldToneStyle = false
        cases.forEach { (raw, expected) -> assertEquals(raw, expected, TelexEngine.translate(raw)) }
    }

    @Test fun oldToneStylePutsToneOnFirstVowel() {
        TelexEngine.oldToneStyle = true
        assertEquals("hòa", TelexEngine.translate("hoaf"))
        assertEquals("thủy", TelexEngine.translate("thuyr"))
        TelexEngine.oldToneStyle = false
    }
}
