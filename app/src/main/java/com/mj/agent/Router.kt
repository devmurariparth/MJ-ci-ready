package com.mj.agent

import org.json.JSONObject

/** Local fast-path for common phone, media and app commands. */
object Router {
    private val taskWords = listOf(
        // English
        "open ", "launch ", "call ", "dial ", "phone ", "make a call", "send ", "whatsapp",
        "alarm", "timer", "flashlight", "torch", "turn on", "turn off", "switch on", "switch off",
        "play ", "listen to ", "song", "music", "search ", "google ", "scroll", "click ", "tap ",
        "volume", "mute", "unmute", "next song", "previous song", "pause music", "resume music", "simpmusic",
        "type ", "go back", "go home", "wifi", "bluetooth", "remind", "set a ", "set an ",
        // Roman Gujarati / Hindi
        "kholo", "khol ", "khol do", "mokal", "moklo", "bhejo", "bhej do", "chalu", "bandh kar",
        "band kar", "call kar", "phone kar", "phone karo", "alarm laga", "laga do", "lagao",
        "shodh", "khojo", "chalao", "vagado", "vagadu", "spotify", "youtube", "simpmusic", "awaj", "avaj", "ochho",
        // Gujarati
        "ખોલ", "મોકલ", "ફોન કર", "કોલ", "અલાર્મ", "ટાઈમર", "ટાઇમર", "ટોર્ચ", "ચાલુ",
        "બંધ કર", "શોધ", "સર્ચ", "વગાડ", "ગીત", "સોંગ", "સ્પોટિફાય", "યુટ્યુબ", "સિમ્પ મ્યુઝિક", "વોલ્યુમ", "આવાજ", "ઓછો", "આગળનું ગીત", "પાછળનું ગીત", "સેટ કર",
        // Hindi
        "खोल", "भेज", "फोन कर", "कॉल", "अलार्म", "टाइमर", "टॉर्च", "चालू", "बंद कर",
        "खोज", "सर्च", "चला", "बजा", "गाना", "गीत", "स्पॉटिफाई", "यूट्यूब", "सिम्प म्यूजिक", "आवाज़", "कम", "अगला गाना", "पिछला गाना", "सेट कर"
    )

    fun looksLikeTask(text: String): Boolean {
        val t = " " + text.lowercase() + " "
        return taskWords.any { t.contains(it) }
    }

    private val torchRx = Regex("torch|flash ?light|ટોર્ચ|ફ્લેશલાઇટ|ફ્લેશલાઈટ|टॉर्च|टार्च|फ्लैशलाइट")
    private val offRx = Regex("\\boff\\b|\\bband\\b|\\bbandh\\b|બંધ|बंद")
    private val openEn = Regex("^(?:please\\s+)?(?:open|launch|start)\\s+(?:the\\s+)?(.+?)(?:\\s+app)?$")
    private val openIn = Regex("^(.+?)\\s+(?:ખોલી દે|ખોલી આપો|ખોલો|ખોલ|खोल दो|खोलिए|खोलें|खोलो|खोल|kholo|khol do|khol de|khol|open kar|open karo|chalu kar)$")

    private fun callTarget(t: String): String? {
        val patterns = listOf(
            Regex("^(?:please\\s+)?(?:call|dial|phone)\\s+(.+)$"),
            Regex("^make\\s+(?:a\\s+)?(?:phone\\s+)?call\\s+to\\s+(.+)$"),
            Regex("^(?:call|phone)\\s+kar\\s+(.+)$"),
            Regex("^(.+?)\\s+ne\\s+(?:phone|call)\\s+(?:kar|karo|karjo|lagao|lagav)$"),
            Regex("^(.+?)\\s+ko\\s+(?:phone|call)\\s+(?:kar|karo|kar\\s+do|lagao)$"),
            Regex("^(.+?)(?:ને| ને)\\s*(?:ફોન|કોલ)\\s*(?:કર|કરો|કરજો|લગાવો|લગાડો)$"),
            Regex("^(?:મને\\s+)?(.+?)\\s+(?:ફોન|કોલ)\\s+(?:કર|કરો|કરજો)$"),
            Regex("^(.+?)\\s+को\\s+(?:कॉल|फोन)\\s+(?:करो|करें|लगाओ)$"),
            Regex("^(?:कॉल|फोन)\\s+करो?\\s+(.+)$")
        )
        for (pattern in patterns) {
            val match = pattern.matchEntire(t) ?: continue
            val target = match.groupValues[1].trim()
                .removePrefix("the ").removePrefix("my ").trim()
            if (target.isNotBlank()) return target
        }
        return null
    }

    private fun musicAction(t: String): Act? {
        val hasPlayIntent = listOf(
            "play ", "listen to ", "put on ", "chalao", "chala", "vagado", "vagadu",
            "bajao", "વગાડ", "ચાલાવો", "બજાવો", "चलाओ", "बजाओ"
        ).any { t.contains(it) }
        if (!hasPlayIntent) return null

        val provider = when {
            t.contains("simpmusic") || t.contains("simp music") || t.contains("સિમ્પ મ્યુઝિક") || t.contains("सिम्प म्यूजिक") -> "simpmusic"
            t.contains("spotify") || t.contains("સ્પોટિફાય") || t.contains("स्पॉटिफाई") -> "spotify"
            t.contains("youtube music") -> "youtube music"
            t.contains("youtube") || t.contains("યુટ્યુબ") || t.contains("यूट्यूब") -> "youtube"
            else -> "auto"
        }

        var query = t
        val prefixes = listOf(
            "please play ", "play ", "listen to ", "put on ", "મને ", "મારે ", "આ ગીત ",
            "मुझे ", "गाना चलाओ ", "गाना बजाओ "
        )
        for (prefix in prefixes) if (query.startsWith(prefix)) {
            query = query.removePrefix(prefix)
            break
        }
        query = query.replace(Regex("^(?:spotify|youtube(?: music)?|સ્પોટિફાય|યુટ્યુબ|स्पॉटिफाई|यूट्यूब)(?:\\s+(?:ma|par|pe|માં|પર|में|पर))?\\s+"), "")
        query = query.replace(Regex("\\s+(?:on|in)\\s+(?:spotify|simpmusic|simp music|youtube(?: music)?)$"), "")
        query = query.replace(Regex("\\s+(?:spotify|simpmusic|simp music|youtube(?: music)?)(?:\\s+(?:ma|par|pe|માં|પર|में|पर))?$"), "")
        query = query.replace(
            Regex("\\s+(?:please|song|songs|ગીત|ગીતો|સોંગ|ગાના|गाना|गाने|ગીતને|વગાડો?|વગાડ|ચલાવો?|બજાવો|chalao|chala|vagado|vagadu|bajao)$"),
            ""
        ).trim()
        query = query.trim(' ', '.', ',', '!', '?', '।')
        if (query.isBlank() || query in setOf("music", "song", "songs", "ગીત", "ગીતો", "गाना", "गाने")) {
            query = "music"
        }
        return Act("play_music", JSONObject().put("query", query).put("provider", provider))
    }

    fun quick(text: String): Act? {
        val t = text.trim().trimEnd('.', '!', '?', '।').lowercase()
        if (t.isEmpty() || t.length > 120) return null

        if (torchRx.containsMatchIn(t)) {
            val off = offRx.containsMatchIn(t)
            return Act("flashlight", JSONObject().put("on", !off))
        }

        val callTo = callTarget(t)
        if (callTo != null) return Act("call", JSONObject().put("to", callTo))

        musicAction(t)?.let { return it }

        val name = openEn.find(t)?.groupValues?.get(1) ?: openIn.find(t)?.groupValues?.get(1)
        if (name != null && name.split(Regex("\\s+")).size <= 3 && Apps.resolve(name, strict = true) != null) {
            return Act("open_app", JSONObject().put("app", name))
        }
        return null
    }
}
