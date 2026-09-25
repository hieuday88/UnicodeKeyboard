// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.unicode

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

/** Offline English <-> Vietnamese translation backed by ML Kit on-device models. */
object TextTranslator {
    private const val VIETNAMESE_ONLY_CHARS = "ăâđêôơưàáảãạằắẳẵặầấẩẫậèéẻẽẹềếểễệìíỉĩịòóỏõọồốổỗộờớởỡợùúủũụừứửữựỳýỷỹỵ"

    private val viToEn: Translator by lazy { client(TranslateLanguage.VIETNAMESE, TranslateLanguage.ENGLISH) }
    private val enToVi: Translator by lazy { client(TranslateLanguage.ENGLISH, TranslateLanguage.VIETNAMESE) }

    private fun client(source: String, target: String) = Translation.getClient(
        TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build()
    )

    /** Returns true when [text] contains Vietnamese-specific letters, so it should be translated to English. */
    fun isVietnamese(text: CharSequence) = text.any { it.lowercaseChar() in VIETNAMESE_ONLY_CHARS }

    /** Translates [text] in the detected direction; downloads the model on first use. Calls back on the main thread. */
    fun translate(text: String, onResult: (Result<String>) -> Unit) {
        val translator = if (isVietnamese(text)) viToEn else enToVi
        translator.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .onSuccessTask { translator.translate(text) }
            .addOnSuccessListener { onResult(Result.success(it)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Checks whether both the English and Vietnamese models are on the device. */
    fun areModelsDownloaded(onResult: (Boolean) -> Unit) {
        RemoteModelManager.getInstance().getDownloadedModels(TranslateRemoteModel::class.java)
            .addOnSuccessListener { models ->
                val languages = models.map { it.language }
                onResult(TranslateLanguage.VIETNAMESE in languages && TranslateLanguage.ENGLISH in languages)
            }
            .addOnFailureListener { onResult(false) }
    }

    /** Downloads the Vietnamese and English models. */
    fun downloadModels(onResult: (Result<Unit>) -> Unit) {
        enToVi.downloadModelIfNeeded(DownloadConditions.Builder().build())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }

    /** Deletes the Vietnamese model to free storage. The English model is part of ML Kit and cannot be removed. */
    fun deleteModels(onResult: (Result<Unit>) -> Unit) {
        RemoteModelManager.getInstance()
            .deleteDownloadedModel(TranslateRemoteModel.Builder(TranslateLanguage.VIETNAMESE).build())
            .addOnSuccessListener { onResult(Result.success(Unit)) }
            .addOnFailureListener { onResult(Result.failure(it)) }
    }
}
