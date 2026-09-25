// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.unicode

import android.view.View
import android.widget.ImageButton
import androidx.core.view.isInvisible
import helium314.keyboard.keyboard.KeyboardActionListener
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.R
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.settings.Settings

/** iOS-style bar below the keys holding the language switch (globe) and voice input (mic) buttons. */
object IosBottomBar {
    /** Wires the globe and mic buttons of [inputView] to [listener]. */
    fun setup(inputView: View, listener: KeyboardActionListener) {
        bind(inputView.findViewById(R.id.ios_globe_button) ?: return, KeyCode.LANGUAGE_SWITCH, listener)
        bind(inputView.findViewById(R.id.ios_mic_button) ?: return, KeyCode.VOICE_INPUT, listener)
        refresh(inputView)
    }

    /** Applies theme colors and shows each button only when its action is available in the current field. */
    fun refresh(inputView: View) {
        val values = Settings.getValues() ?: return
        inputView.findViewById<ImageButton>(R.id.ios_globe_button)?.let {
            values.mColors.setColor(it, ColorType.KEY_ICON)
            it.isInvisible = !values.isLanguageSwitchKeyEnabled
        }
        inputView.findViewById<ImageButton>(R.id.ios_mic_button)?.let {
            values.mColors.setColor(it, ColorType.KEY_ICON)
            it.isInvisible = !values.mShowsVoiceInputKey
        }
    }

    private fun bind(button: ImageButton, code: Int, listener: KeyboardActionListener) {
        button.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
            listener.onCodeInput(code, Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE, false)
        }
    }
}
