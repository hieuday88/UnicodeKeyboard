// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.unicode

/** Converts a raw Telex keystroke sequence of one word into Vietnamese text. Ported from CMkey TelexEngine. */
object TelexEngine {
    private val vowelMap: Map<Char, CharArray> = mapOf(
        'a' to "aáàảãạ", 'ă' to "ăắằẳẵặ", 'â' to "âấầẩẫậ", 'e' to "eéèẻẽẹ", 'ê' to "êếềểễệ",
        'i' to "iíìỉĩị", 'o' to "oóòỏõọ", 'ô' to "ôốồổỗộ", 'ơ' to "ơớờởỡợ", 'u' to "uúùủũụ",
        'ư' to "ưứừửữự", 'y' to "yýỳỷỹỵ",
        'A' to "AÁÀẢÃẠ", 'Ă' to "ĂẮẰẲẴẶ", 'Â' to "ÂẤẦẨẪẬ", 'E' to "EÉÈẺẼẸ", 'Ê' to "ÊẾỀỂỄỆ",
        'I' to "IÍÌỈĨỊ", 'O' to "OÓÒỎÕỌ", 'Ô' to "ÔỐỒỔỖỘ", 'Ơ' to "ƠỚỜỞỠỢ", 'U' to "UÚÙỦŨỤ",
        'Ư' to "ƯỨỪỬỮỰ", 'Y' to "YÝỲỶỸỴ",
    ).mapValues { it.value.toCharArray() }

    private val inverseVowelMap: Map<Char, Pair<Char, Int>> = buildMap {
        for ((base, arr) in vowelMap) arr.forEachIndexed { tone, c -> put(c, base to tone) }
    }

    private const val VOWELS_LOWER = "aeiouyăâêôơư"
    private const val TONE_CHARS = "sfrxj"
    private const val HOOK_BASES = "ươăƯƠĂ"

    /** When true, tones in open "oa", "oe", "uy" go on the first vowel (hòa) instead of the second (hoà). */
    @Volatile var oldToneStyle = false

    private fun hasVowel(s: CharSequence) = s.any { it.lowercaseChar() in VOWELS_LOWER }
    private fun isVowel(c: Char) = c.lowercaseChar() in VOWELS_LOWER

    /** Converts every space-separated word of [rawInput]. */
    fun process(rawInput: String): String = rawInput.split(' ').joinToString(" ") { translate(it) }

    /** Converts one raw word, handling "undo" sequences like "ss" or "aaa" that restore the literal letters. */
    fun translate(raw: String): String {
        val len = raw.length
        if (len >= 3) {
            val last3 = raw.substring(len - 3).lowercase()
            if (last3 == "ooo" || last3 == "eee" || last3 == "aaa" || last3 == "ddd")
                return translate(raw.substring(0, len - 3)) + raw[len - 2] + raw[len - 1]
            if (last3 == "aww" || last3 == "oww" || last3 == "uww")
                return translate(raw.substring(0, len - 3)) + raw[len - 3] + raw[len - 2]
        }
        if (len >= 2) {
            val last2 = raw.substring(len - 2).lowercase()
            if (last2 == "ss" || last2 == "ff" || last2 == "rr" || last2 == "xx" || last2 == "jj")
                return translate(raw.substring(0, len - 2)) + raw[len - 1]
        }
        return processSmartTelex(raw)
    }

    private fun processSmartTelex(raw: String): String {
        if (raw.isEmpty()) return ""
        val isFirstCharUpper = raw[0].isUpperCase()
        val isAllUpper = raw.length > 1 && raw.all { it.isUpperCase() }
        var word = raw.lowercase()
            .replace("uoow", "ươu")
            .replace("uwu", "ưu").replace("uuw", "ưu")
            .replace("owo", "ơo").replace("oow", "ơo")
            .replace("uow", "ươ").replace("ouw", "ươ")

        var activeTone = 0
        var hasHook = false
        word = buildString {
            for (c in word) {
                val mapping = inverseVowelMap[c]
                if (mapping == null) { append(c); continue }
                append(mapping.first)
                if (mapping.second > 0) activeTone = mapping.second
                if (mapping.first in HOOK_BASES) hasHook = true
            }
        }

        val base = StringBuilder()
        for (i in word.indices) {
            val c = word[i]
            val prefixHasVowel = hasVowel(word.substring(0, i))
            if (prefixHasVowel && c in TONE_CHARS && i > 0 && !(c == 'r' && i == 1 && word[0] == 't')) {
                val tone = TONE_CHARS.indexOf(c) + 1
                if (activeTone == tone) { activeTone = 0; base.append(c) } else activeTone = tone
            } else if (prefixHasVowel && c == 'w' && i > 0) {
                if (word[i - 1] == 'w') { hasHook = false; base.append(c) } else hasHook = true
            } else base.append(c)
        }

        var baseWord = base.toString()
            .replace("aa", "â").replace("ee", "ê").replace("oo", "ô").replace("dd", "đ")
            .replace("âa", "aa").replace("êe", "ee").replace("ôo", "oo").replace("đd", "dd")
        if (hasHook) baseWord = applyHookToVowelCluster(baseWord)
        if (activeTone > 0) baseWord = applyTone(baseWord, activeTone)

        return when {
            isAllUpper -> baseWord.uppercase()
            isFirstCharUpper && baseWord.isNotEmpty() -> baseWord[0].uppercaseChar() + baseWord.substring(1)
            else -> baseWord
        }
    }

    private fun toBase(c: Char) = when (c) {
        'ă', 'â' -> 'a'
        'ê' -> 'e'
        'ô', 'ơ' -> 'o'
        'ư' -> 'u'
        else -> c
    }

    private fun applyHookToSeq(vowelSeq: String): String {
        val baseSeq = vowelSeq.map(::toBase).joinToString("")
        for (pair in arrayOf("uo", "oa", "ua", "oe", "oo")) {
            val idx = baseSeq.indexOf(pair)
            if (idx >= 0) {
                val rep = when (pair) { "uo" -> "ươ"; "oa" -> "oă"; "ua" -> "ưa"; "oe" -> "oe"; else -> "ơo" }
                return StringBuilder(vowelSeq).replace(idx, idx + 2, rep).toString()
            }
        }
        var hookedU = false
        val sb = StringBuilder(vowelSeq)
        for (i in vowelSeq.indices) {
            when (toBase(vowelSeq[i])) {
                'u' -> { sb.setCharAt(i, if (hookedU) 'u' else 'ư'); hookedU = true }
                'o' -> sb.setCharAt(i, 'ơ')
                'a' -> sb.setCharAt(i, 'ă')
            }
        }
        return sb.toString()
    }

    private fun vowelStartIndex(word: String): Int {
        val lower = word.lowercase()
        if (lower.startsWith("qu") && word.length > 2) return 2
        if (lower.startsWith("gi") && word.length > 2 && (2 until word.length).any { isVowel(word[it]) }) return 2
        return 0
    }

    private fun applyHookToVowelCluster(word: String): String {
        var first = -1
        var last = -1
        for (i in vowelStartIndex(word) until word.length) {
            if (isVowel(word[i])) { if (first == -1) first = i; last = i } else if (first != -1) break
        }
        if (first == -1) return word
        val modified = applyHookToSeq(word.substring(first, last + 1).lowercase())
        return StringBuilder(word).replace(first, last + 1, modified).toString()
    }

    private fun applyTone(word: String, tone: Int): String {
        val vowels = (vowelStartIndex(word) until word.length).filter { isVowel(word[it]) }.map { it to word[it] }
        if (vowels.isEmpty()) return word
        val target = when (vowels.size) {
            1 -> vowels[0]
            2 -> {
                if (vowels[1].first < word.length - 1) vowels[1]
                else {
                    val pair = "${vowels[0].second.lowercaseChar()}${vowels[1].second.lowercaseChar()}"
                    val newStylePair = pair == "oa" || pair == "oe" || pair == "uy"
                    val toneOnSecond = (newStylePair && !oldToneStyle) || pair == "uơ" || pair == "uê"
                    if (toneOnSecond) vowels[1] else vowels[0]
                }
            }
            else -> findTriphthongTarget(vowels) ?: vowels[1]
        }
        val accented = vowelMap[target.second] ?: return word
        if (tone >= accented.size) return word
        return StringBuilder(word).apply { setCharAt(target.first, accented[tone]) }.toString()
    }

    private fun normBase(c: Char) = when (val lc = c.lowercaseChar()) {
        'ă', 'â' -> 'a'
        'ê' -> 'E'
        'ô' -> 'O'
        'ơ' -> 'R'
        'ư' -> 'U'
        else -> lc
    }

    private fun findTriphthongTarget(vowels: List<Pair<Int, Char>>): Pair<Int, Char>? {
        val cluster = vowels.map { normBase(it.second) }.joinToString("")
        fun at(n: Int) = vowels.getOrNull(n)
        if (cluster.length < 3) return null
        val (c0, c1, c2) = Triple(cluster[0], cluster[1], cluster[2])
        if (c0 == 'u' && c1 == 'y' && c2 == 'E') return at(2)
        if ((c0 == 'i' || c0 == 'y') && c1 == 'E' && c2 == 'u') return at(1)
        if (c0 == 'u' && c1 == 'O' && c2 == 'i') return at(1)
        if (c0 == 'R' && (c2 == 'i' || c2 == 'u')) {
            val rPos = vowels.indexOfFirst { normBase(it.second) == 'R' }
            if (rPos >= 0) return at(rPos)
        }
        if (c0 == 'o' && c1 == 'a' && (c2 == 'i' || c2 == 'y' || c2 == 'o')) return at(1)
        if (cluster == "oeo") return at(1)
        if (c0 == 'u' && c1 == 'a' && c2 == 'y') return at(1)
        return null
    }
}
