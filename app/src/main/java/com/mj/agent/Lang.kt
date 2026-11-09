package com.mj.agent

enum class Lang(val label: String, val tag: String, val instruction: String) {
    GU("ગુજરાતી", "gu-IN", "Reply ONLY in Gujarati script (ગુજરાતી)."),
    HI("हिन्दी", "hi-IN", "Reply ONLY in Hindi, Devanagari script (हिन्दी)."),
    EN("English", "en-IN", "Reply ONLY in English.")
}

private val guMarkers = setOf("kem", "cho", "chhe", "che", "shu", "mane", "tame", "tamne", "hu", "mari", "tari", "nathi", "joiye", "aapo", "apo", "ketla", "kyare", "moklo")
private val hiMarkers = setOf("hai", "kaise", "mujhe", "kya", "nahi", "mera", "karo", "batao", "kyun", "kaun", "bhejo", "mujhko")

/** Script thi bhasha oLakhvi. Roman Gujarati/Hindi mate marker shabdo. */
fun detectLang(text: String): Lang {
    var gu = 0
    var hi = 0
    for (c in text) {
        val code = c.code
        if (code in 0x0A80..0x0AFF) gu++ else if (code in 0x0900..0x097F) hi++
    }
    if (gu > 0 || hi > 0) return if (gu >= hi) Lang.GU else Lang.HI
    val words = text.lowercase().split(Regex("[^a-z]+")).filter { it.isNotEmpty() }
    val g = words.count { it in guMarkers }
    val h = words.count { it in hiMarkers }
    return when {
        g > h && g > 0 -> Lang.GU
        h > g && h > 0 -> Lang.HI
        else -> Lang.EN
    }
}

/** Gemma chat template. History hamesha user thi sharu thay che. */
fun buildChatPrompt(history: List<Pair<Boolean, String>>, userText: String, lang: Lang): String {
    val sys = "You are MJ, a smart, friendly and precise AI assistant. " +
        "Understand Gujarati, Hindi and English, also when written in Roman letters. " +
        lang.instruction + " Keep answers short, clear and correct."
    var h = history.takeLast(6)
    while (h.isNotEmpty() && !h.first().first) h = h.drop(1)
    val sb = StringBuilder()
    sb.append("<start_of_turn>user\n").append(sys).append("\n\n")
    if (h.isEmpty()) {
        sb.append(userText).append("<end_of_turn>\n")
    } else {
        h.forEachIndexed { i, pair ->
            if (pair.first) {
                if (i > 0) sb.append("<start_of_turn>user\n")
                sb.append(pair.second).append("<end_of_turn>\n")
            } else {
                sb.append("<start_of_turn>model\n").append(pair.second).append("<end_of_turn>\n")
            }
        }
        sb.append("<start_of_turn>user\n").append(userText).append("<end_of_turn>\n")
    }
    sb.append("<start_of_turn>model\n")
    return sb.toString()
}
