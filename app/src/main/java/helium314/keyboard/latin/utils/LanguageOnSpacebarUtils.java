/*
 * Copyright (C) 2014 The Android Open Source Project
 * modified
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */

package helium314.keyboard.latin.utils;

import android.view.inputmethod.InputMethodSubtype;

import androidx.annotation.NonNull;

import helium314.keyboard.latin.RichInputMethodSubtype;
import helium314.keyboard.latin.settings.Settings;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * This class determines that the language name on the spacebar should be displayed in what format.
 */
public final class LanguageOnSpacebarUtils {
    public static final int FORMAT_TYPE_NONE = 0;
    public static final int FORMAT_TYPE_LANGUAGE_ONLY = 1;
    public static final int FORMAT_TYPE_FULL_LOCALE = 2;

    private static List<InputMethodSubtype> sEnabledSubtypes = Collections.emptyList();
    private static boolean sIsSystemLanguageSameAsInputLanguage;

    private LanguageOnSpacebarUtils() {
        // This utility class is not publicly instantiable.
    }

    /** Always shows the iOS-style space label ("dấu cách" / "space"), whatever subtypes are enabled. */
    public static int getLanguageOnSpacebarFormatType(@NonNull final RichInputMethodSubtype subtype) {
        return FORMAT_TYPE_FULL_LOCALE;
    }

    public static void setEnabledSubtypes(@NonNull final List<InputMethodSubtype> enabledSubtypes) {
        sEnabledSubtypes = enabledSubtypes;
    }

    public static void onSubtypeChanged(@NonNull final RichInputMethodSubtype subtype,
           final boolean implicitlyEnabledSubtype, @NonNull final Locale systemLocale) {
        final Locale newLocale = subtype.getLocale();
        if (systemLocale.equals(newLocale)) {
            sIsSystemLanguageSameAsInputLanguage = true;
            return;
        }
        if (!systemLocale.getLanguage().equals(newLocale.getLanguage())) {
            sIsSystemLanguageSameAsInputLanguage = false;
            return;
        }
        // If the subtype is enabled explicitly, the language name should be displayed even when
        // the keyboard language and the system language are equal.
        sIsSystemLanguageSameAsInputLanguage = implicitlyEnabledSubtype;
    }
}
