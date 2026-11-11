package com.mj.agent

import org.json.JSONObject

/** Local fast-path for common phone, media and app commands. */
object Router {
    private val taskWords = listOf(
        // English
        "open ", "launch ", "call ", "dial ", "phone ", "make a call", "send ", "whatsapp",
        "alarm", "timer", "flashlight", "torch", "turn on", "turn off", "switch on", "switch off",
        "play ", "listen to ", "song", "music", "search ", "google ", "scroll", "click ", "tap ", "brightness", "screen brightness", "rotate screen", "rotation",
        "volume", "mute", "unmute", "next song", "previous song", "pause music", "resume music", "simpmusic",
        "type ", "go back", "go home", "wifi", "bluetooth", "remind", "set a ", "set an ", "screenshot", "take screenshot",
        // Roman Gujarati / Hindi
        "kholo", "khol ", "khol do", "mokal", "moklo", "bhejo", "bhej do", "chalu", "bandh kar",
        "band kar", "call kar", "phone kar", "phone karo", "alarm laga", "laga do", "lagao",
        "shodh", "khojo", "chalao", "vagado", "vagadu", "spotify", "youtube", "simpmusic", "awaj", "avaj", "ochho", "screen brightness", "brightness", "screenshot", "photo kholo",
        // Gujarati
        "ખોલ", "મોકલ", "ફોન કર", "કોલ", "અલાર્મ", "ટાઈમર", "ટાઇમર", "ટોર્ચ", "ચાલુ",
        "બંધ કર", "શોધ", "સર્ચ", "વગાડ", "બ્રાઇટનેસ", "સ્ક્રીનશોટ", "સ્ક્રીન શોટ", "ગીત", "સોંગ", "સ્પોટિફાય", "યુટ્યુબ", "સિમ્પ મ્યુઝિક", "વોલ્યુમ", "આવાજ", "ઓછો", "આગળનું ગીત", "પાછળનું ગીત", "સેટ કર",
        // Hindi
        "खोल", "भेज", "फोन कर", "कॉल", "अलार्म", "टाइमर", "टॉर्च", "चालू", "बंद कर", "ब्राइटनेस", "स्क्रीनशॉट",
        "खोज", "सर्च", "चला", "बजा", "गाना", "गीत", "स्पॉटिफाई", "यूट्यूब", "सिम्प म्यूजिक", "आवाज़", "कम", "अगला गाना", "पिछला गाना", "सेट कर"
    )

    fun looksLikeTask(text: String): Boolean {
        val t = " " + text.lowercase() + " "
        return taskWords.any { t.contains(it) }
    }

    private val torchRx = Regex("torch|flash ?light|ટોર્ચ|ફ્લેશલાઇટ|ફ્લેશલાઈટ|टॉर्च|टार्च|फ्लैशलाइट")
    private val offRx = Regex("\\boff\\b|\\bband\\b|\\bbandh\\b|બંધ|बंद")
    private val onRx = Regex("\\bon\\b|\\bchalu\\b|\\bchaalu\\b|ચાલુ|चालू")
    private val questionRx = Regex(
        "^(?:what\\b|why\\b|how\\b|when\\b|where\\b|who\\b|which\\b|explain\\b|" +
            "tell me (?:about|why|how)\\b|શું\\b|શા માટે|કેમ\\b|ક્યારે\\b|કોણ\\b|કેવી રીતે|" +
            "क्या\\b|क्यों\\b|कैसे\\b|कब\\b|कहाँ\\b|कौन\\b)"
    )
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
        val startsWithPlayIntent = listOf("please play ", "play ", "listen to ", "put on ")
            .any(t::startsWith)
        val endsWithPlayIntent = listOf(
            "chalao", "chala", "vagado", "vagadu", "bajao",
            "વગાડ", "ચાલાવો", "બજાવો", "चलाओ", "बजाओ"
        ).any(t::endsWith)
        val hasPlayIntent = startsWithPlayIntent || endsWithPlayIntent
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

    private fun mediaControlAction(t: String): Act? {
        val action = when {
            listOf("next song", "next track", "skip song", "skip track", "aagal nu song", "bija song",
                "આગળનું ગીત", "આગળનું ગાણું", "આગળનું સોંગ", "अगला गाना", "अगला गीत").any(t::contains) -> "next"
            listOf("previous song", "previous track", "last song", "pachhal nu song",
                "પહેલાનું ગીત", "પાછળનું ગીત", "પાછળનું ગાણું", "पिछला गाना", "पिछला गीत").any(t::contains) -> "previous"
            listOf("pause music", "pause song", "stop music", "music stop", "song bandh",
                "ગીત બંધ", "સંગીત બંધ", "ગાણું બંધ", "गाना रोक", "गाना बंद", "संगीत रोक").any(t::contains) -> "pause"
            listOf("resume music", "resume song", "continue music", "continue song", "music chalu",
                "ગીત ચાલુ", "સંગીત ચાલુ", "गाना चालू", "संगीत चालू").any(t::contains) -> "resume"
            else -> null
        } ?: return null
        return Act("media_control", JSONObject().put("action", action))
    }

    private fun volumeAction(t: String): Act? {
        val hasVolumeCue = listOf("volume", "awaj", "avaj", "avaz", "sound", "mute", "unmute",
            "વોલ્યુમ", "આવાજ", "आवाज़", "आवाज").any(t::contains)
        if (!hasVolumeCue) return null
        val numeric = Regex("""(?:volume|વોલ્યુમ|આવાજ|आवाज़|आवाज).*?(\d{1,3})\s*%?""").find(t)
            ?: Regex("""(\d{1,3})\s*(?:percent|પ્રતિશત|प्रतिशत|%)""").find(t)
        val mode = when {
            numeric != null -> "set:" + (numeric.groupValues[1].toIntOrNull() ?: 0)
            listOf("unmute", "un-mute", "sound on", "awaj chalu", "આવાજ ચાલુ", "आवाज़ चालू").any(t::contains) -> "unmute"
            listOf("mute", "silent", "zero", "sound off", "volume off", "આવાજ બંધ", "आवाज़ बंद").any(t::contains) -> "mute"
            listOf("full", "maximum", "max").any(t::contains) -> "max"
            listOf("down", "decrease", "lower", "low", "less", "ochho", "ochu", "ocho", "ghatad",
                "ઓછો", "ઘટાડ", "कम", "घटाओ").any(t::contains) -> "down"
            listOf("up", "increase", "raise", "higher", "louder", "vadharo", "vadhaaro", "badhao",
                "વધાર", "વધારો", "बढ़ा", "ज्यादा").any(t::contains) -> "up"
            else -> return null
        }
        return Act("volume", JSONObject().put("mode", mode))
    }

    private fun alarmOrTimerAction(t: String): Act? {
        val alarmCue = listOf("alarm", "અલાર્મ", "અલારમ", "अलार्म").any(t::contains)
        val alarmVerb = listOf("set", "create", "start", "laga", "lagao", "laga do", "kar", "karo",
            "muk", "muko", "મૂક", "સેટ કર", "લગાવ", "लगाओ", "लगाकर").any(t::contains)
        if (alarmCue && alarmVerb) {
            val match = Regex("""(?:^|\s)(\d{1,2})(?:[:.](\d{2})|\s+(\d{2}))?\s*(am|pm)?(?=\s|$)""").find(t)
            if (match != null) {
                var hour = match.groupValues[1].toIntOrNull() ?: return null
                val minute = (match.groupValues[2].ifBlank { match.groupValues[3] }).toIntOrNull() ?: 0
                val suffix = match.groupValues[4]
                if (minute !in 0..59) return null
                if (suffix.isNotBlank()) {
                    if (hour !in 1..12) return null
                    if (suffix == "am" && hour == 12) hour = 0
                    if (suffix == "pm" && hour in 1..11) hour += 12
                }
                if (hour !in 0..23) return null
                return Act("alarm", JSONObject().put("hour", hour).put("minute", minute).put("label", "MJ"))
            }
        }

        val timerCue = listOf("timer", "ટાઈમર", "ટાઇમર", "टाइमर").any(t::contains)
        if (timerCue) {
            // Use bounded Long arithmetic so unusually large spoken values cannot overflow Int.
            val hours = Regex("""(\d+)\s*(?:hours?|hrs?|hr|h|કલાક|घंटे|घंटा)""")
                .find(t)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(169L) ?: 0L
            val minutes = Regex("""(\d+)\s*(?:minutes?|mins?|min|મિનિટ|मिनट|मिनटों)""")
                .find(t)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(10081L) ?: 0L
            val seconds = Regex("""(\d+)\s*(?:seconds?|secs?|sec|સેકન્ડ|सेकंड)""")
                .find(t)?.groupValues?.get(1)?.toLongOrNull()?.coerceAtMost(604801L) ?: 0L
            val hasUnits = hours > 0L || minutes > 0L || seconds > 0L
            val total = if (hasUnits) {
                hours * 3600L + minutes * 60L + seconds
            } else {
                Regex("""(?:timer.*?(\d+)|(\d+).*?timer)""").find(t)?.let {
                    (it.groupValues[1].ifBlank { it.groupValues[2] })
                        .toLongOrNull()?.coerceAtMost(10081L)?.times(60L)
                } ?: 0L
            }
            if (total in 1L..(7L * 24L * 60L * 60L)) {
                return Act("timer", JSONObject().put("seconds", total.toInt()))
            }
        }
        return null
    }

    fun quick(text: String): Act? {
        val t = text.trim().trimEnd('.', '!', '?', '।').lowercase()
        if (t.isEmpty() || t.length > 120 || questionRx.containsMatchIn(t)) return null

        if (torchRx.containsMatchIn(t)) {
            val off = offRx.containsMatchIn(t)
            val on = onRx.containsMatchIn(t)
            if (!off && !on) return null
            return Act("flashlight", JSONObject().put("on", !off))
        }

        // Keep common device settings on the deterministic local path.
        if (listOf("brightness", "screen brightness", "બ્રાઇટનેસ", "ब्राइटनेस").any(t::contains)) {
            val value = Regex("""(\d{1,3})\s*%?""").find(t)?.groupValues?.get(1)?.toIntOrNull()
            val mode = when {
                listOf("max", "maximum", "full", "વધુ", "ज्यादा").any(t::contains) -> "max"
                listOf("min", "minimum", "low", "ઓછું", "कम").any(t::contains) -> "min"
                listOf("down", "decrease", "ઘટાડ", "कम कर").any(t::contains) -> "down"
                listOf("up", "increase", "વધાર", "बढ़ा").any(t::contains) -> "up"
                value != null -> "set:" + value.coerceIn(0, 100)
                else -> return null
            }
            return Act("brightness", JSONObject().put("mode", mode))
        }

        val screenshotCue = listOf("screenshot", "screen shot", "સ્ક્રીનશોટ", "સ્ક્રીન શોટ", "स्क्रीनशॉट").any(t::contains)
        val screenshotIntent = t == "screenshot" || t == "screen shot" ||
            listOf(
                "take screenshot", "capture screenshot", "save screenshot", "screenshot lo", "screenshot le",
                "સ્ક્રીનશોટ લો", "સ્ક્રીનશોટ લઈ", "સ્ક્રીનશોટ લેજો", "સ્ક્રીનશોટ પાડ",
                "स्क्रीनशॉट लो", "स्क्रीनशॉट ले"
            ).any(t::contains)
        if (screenshotCue && screenshotIntent) return Act("screenshot", JSONObject())

        val callTo = callTarget(t)
        if (callTo != null) return Act("call", JSONObject().put("to", callTo))

        mediaControlAction(t)?.let { return it }
        volumeAction(t)?.let { return it }
        alarmOrTimerAction(t)?.let { return it }
        musicAction(t)?.let { return it }

        val name = openEn.find(t)?.groupValues?.get(1) ?: openIn.find(t)?.groupValues?.get(1)
        if (name != null && name.split(Regex("\\s+")).size <= 3 && Apps.resolve(name, strict = true) != null) {
            return Act("open_app", JSONObject().put("app", name))
        }
        return null
    }
}
