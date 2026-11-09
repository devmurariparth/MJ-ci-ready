package com.mj.agent

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Text-to-speech (Android built-in, free). */
object Speaker {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, Lang>? = null

    fun init(ctx: Context) {
        tts = TextToSpeech(ctx.applicationContext) { status ->
            ready = status == TextToSpeech.SUCCESS
            val p = pending
            if (ready && p != null) {
                pending = null
                speak(p.first, p.second)
            }
        }
    }

    fun speak(text: String, lang: Lang) {
        val t = tts
        if (t == null || !ready) {
            pending = text to lang
            return
        }
        val requestedLocale = Locale.forLanguageTag(lang.tag)
        val result = runCatching { t.setLanguage(requestedLocale) }
            .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        if (result < TextToSpeech.LANG_AVAILABLE) {
            runCatching { t.setLanguage(Locale.forLanguageTag("en-IN")) }
        }
        runCatching { t.speak(text.take(600), TextToSpeech.QUEUE_FLUSH, null, "mj") }
    }

    fun stop() {
        try { tts?.stop() } catch (_: Throwable) {}
    }
}
