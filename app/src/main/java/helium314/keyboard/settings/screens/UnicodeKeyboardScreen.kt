// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import helium314.keyboard.unicode.CharacterCatalog
import helium314.keyboard.unicode.TextTranslator
import helium314.keyboard.unicode.UnicodeEngine
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.SwitchPreference

/** Settings for the Unicode Keyboard features: Unicode mode, Telex tone style, translation and Unicode characters. */
@Composable
fun UnicodeKeyboardScreen(onClickBack: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = ctx.prefs()
    var overrides by remember { mutableStateOf(UnicodeEngine.getCustomOverrides()) }
    var modelsReady by remember { mutableStateOf<Boolean?>(null) }
    var modelBusy by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(Unit) {
        UnicodeEngine.load(prefs)
        overrides = UnicodeEngine.getCustomOverrides()
        TextTranslator.areModelsDownloaded { modelsReady = it }
    }

    fun saveOverrides(newOverrides: Map<Char, String>) {
        UnicodeEngine.setCustomOverrides(prefs, newOverrides)
        overrides = UnicodeEngine.getCustomOverrides()
    }

    SearchSettingsScreen(onClickBack = onClickBack, title = stringResource(R.string.unicode_settings_title), settings = emptyList()) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SwitchPreference(
                name = stringResource(R.string.unicode_mode),
                key = UnicodeEngine.PREF_UNICODE_MODE,
                default = false,
                description = stringResource(R.string.unicode_mode_summary),
            )
            SwitchPreference(
                name = stringResource(R.string.unicode_old_tone_title),
                key = Settings.PREF_TELEX_OLD_TONE_STYLE,
                default = false,
                description = stringResource(R.string.unicode_old_tone_summary),
            )

            SectionTitle(stringResource(R.string.unicode_translate_header))
            Preference(
                name = stringResource(R.string.translate),
                description = when {
                    modelBusy -> stringResource(R.string.unicode_translate_downloading)
                    modelsReady == true -> stringResource(R.string.unicode_translate_models_ready)
                    else -> stringResource(R.string.unicode_translate_models_missing)
                },
                onClick = {
                    if (modelBusy) return@Preference
                    modelBusy = true
                    val done: (Result<Unit>) -> Unit = {
                        modelBusy = false
                        TextTranslator.areModelsDownloaded { ready -> modelsReady = ready }
                    }
                    if (modelsReady == true) TextTranslator.deleteModels(done) else TextTranslator.downloadModels(done)
                },
            )

            SectionTitle(stringResource(R.string.unicode_chars_header))
            Preference(name = stringResource(R.string.unicode_chars_default2), onClick = { saveOverrides(UnicodeEngine.default2Overrides) })
            Preference(name = stringResource(R.string.unicode_chars_reset), onClick = { saveOverrides(emptyMap()) })
            CharacterCatalog.entries.keys.forEach { ch ->
                val current = overrides[ch] ?: UnicodeEngine.transformDefault(ch) ?: ch.toString()
                Preference(name = "$ch   →   $current", onClick = { editing = ch })
            }
        }
    }

    editing?.let { ch ->
        CharacterPickerDialog(
            source = ch,
            onDismiss = { editing = null },
            onPick = { replacement ->
                saveOverrides(overrides.toMutableMap().apply { if (replacement == null) remove(ch) else put(ch, replacement) })
                editing = null
            },
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp),
    )
}

/** Lets the user pick a catalog variant, the built-in default or any custom text for [source]; null means default. */
@Composable
private fun CharacterPickerDialog(source: Char, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    var custom by remember { mutableStateOf("") }
    val defaultText = UnicodeEngine.transformDefault(source) ?: source.toString()
    val variants = listOf(source.toString()) + CharacterCatalog.entries[source].orEmpty()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$source") },
        text = {
            Column {
                LazyColumn(Modifier.heightIn(max = 320.dp)) {
                    item { VariantRow("${stringResource(R.string.unicode_chars_use_default)}: $defaultText") { onPick(null) } }
                    items(variants) { variant -> VariantRow(variant) { onPick(variant) } }
                }
                OutlinedTextField(
                    value = custom,
                    onValueChange = { custom = it },
                    label = { Text(stringResource(R.string.unicode_chars_custom_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = custom.isNotEmpty(), onClick = { onPick(custom) }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

@Composable
private fun VariantRow(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
    )
}
