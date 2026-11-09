package com.mj.agent

import org.json.JSONObject

/** LLM vagar fast-path + auto mode ma "kam chhe ke vaat" nakki karvu. */
object Router {
    private val taskWords = listOf(
        // English
        "open ", "launch ", "call ", "dial ", "send ", "whatsapp", "alarm", "timer", "flashlight", "torch",
        "turn on", "turn off", "switch on", "switch off", "play ", "search ", "google ", "scroll", "click ", "tap ",
        "type ", "go back", "go home", "wifi", "bluetooth", "remind", "set a ", "set an ",
        // Roman Gujarati / Hindi
        "kholo", "khol ", "khol do", "mokal", "moklo", "bhejo", "bhej do", "chalu", "bandh kar", "band kar",
        "call kar", "phone kar", "alarm laga", "laga do", "lagao", "shodh", "khojo", "chalao", "vagado",
        // Gujarati
        "ખોલ", "મોકલ", "ફોન કર", "કોલ", "અલાર્મ", "ટાઈમર", "ટાઇમર", "ટોર્ચ", "ચાલુ", "બંધ કર", "શોધ", "સર્ચ", "વગાડ", "સેટ કર",
        // Hindi
        "खोल", "भेज", "फोन कर", "कॉल", "अलार्म", "टाइमर", "टॉर्च", "चालू", "बंद कर", "खोज", "सर्च", "चला", "बजा", "सेट कर"
    )

    fun looksLikeTask(text: String): Boolean {
        val t = " " + text.lowercase() + " "
        return taskWords.any { t.contains(it) }
    }

    private val torchRx = Regex("torch|flash ?light|ટોર્ચ|ફ્લેશલાઇટ|ફ્લેશલાઈટ|टॉर्च|टार्च|फ्लैशलाइट")
    private val offRx = Regex("\\boff\\b|\\bband\\b|\\bbandh\\b|બંધ|बंद")
    private val openEn = Regex("^(?:please\\s+)?(?:open|launch|start)\\s+(?:the\\s+)?(.+?)(?:\\s+app)?$")
    private val openIn = Regex("^(.+?)\\s+(?:ખોલી દે|ખોલી આપો|ખોલો|ખોલ|खोल दो|खोलिए|खोलें|खोलो|खोल|kholo|khol do|khol de|khol|open kar|open karo|chalu kar)$")

    fun quick(text: String): Act? {
        val t = text.trim().trimEnd('.', '!', '?', '।').lowercase()
        if (t.isEmpty() || t.length > 60) return null
        if (torchRx.containsMatchIn(t)) {
            val off = offRx.containsMatchIn(t)
            return Act("flashlight", JSONObject().put("on", !off))
        }
        val name = openEn.find(t)?.groupValues?.get(1) ?: openIn.find(t)?.groupValues?.get(1)
        if (name != null && name.split(Regex("\\s+")).size <= 3 && Apps.resolve(name, strict = true) != null) {
            return Act("open_app", JSONObject().put("app", name))
        }
        return null
    }
}
