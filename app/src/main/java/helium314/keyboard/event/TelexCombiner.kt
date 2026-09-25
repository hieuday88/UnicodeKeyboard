// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.event

import helium314.keyboard.unicode.TelexEngine
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.settings.Settings
import java.util.ArrayList

/**
 * Vietnamese Telex combiner: keeps the raw keystrokes of the current word and shows
 * their Telex conversion as composing text. Password, email and URL fields are left untouched.
 */
class TelexCombiner : Combiner {
    private val raw = StringBuilder()

    override fun processEvent(previousEvents: ArrayList<Event>?, event: Event): Event {
        if (event.keyCode == KeyCode.SHIFT) return event
        if (raw.isEmpty() && !telexAllowedInField()) return event

        if (event.keyCode == KeyCode.DELETE) {
            if (raw.isEmpty()) return event
            raw.deleteCharAt(raw.length - 1)
            if (raw.isEmpty()) {
                reset()
                return Event.createHardwareKeypressEvent(0x20, Constants.CODE_SPACE, 0, event, event.isKeyRepeat)
            }
            return Event.createConsumedEvent(event)
        }

        val codePoint = event.codePoint
        if (!event.isFunctionalKeyEvent && Character.isLetter(codePoint) && Character.charCount(codePoint) == 1) {
            raw.append(codePoint.toChar())
            return Event.createConsumedEvent(event)
        }
        if (raw.isEmpty()) return event
        val converted = combiningStateFeedback
        reset()
        return Event.createSoftwareTextEvent(converted, KeyCode.MULTIPLE_CODE_POINTS, event)
    }

    override val combiningStateFeedback: CharSequence
        get() = TelexEngine.translate(raw.toString())

    override fun reset() {
        raw.setLength(0)
    }

    /** Returns false for password, email, URL and non-text fields, where Telex must not rewrite input. */
    private fun telexAllowedInField(): Boolean {
        val attributes = Settings.getValues()?.mInputAttributes ?: return true
        return attributes.mIsGeneralTextInput && !attributes.mIsPasswordField
    }
}
