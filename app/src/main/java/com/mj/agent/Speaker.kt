package com.mj.agent

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Text-to-speech (Android built-in, free). Thread-safe lifecycle and single pending reply. */
object Speaker {
    private val lock = Any()
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pending: Pair<String, Lang>? = null

    fun init(ctx: Context) {
        synchronized(lock) {
            if (tts != null) return
            val appContext = ctx.applicationContext
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(appContext) { status ->
                val queued: Pair<String, Lang>?
                synchronized(lock) {
                    // Ignore callbacks from an engine that has already been replaced.
                    if (tts !== engine) return@TextToSpeech
                    ready = status == TextToSpeech.SUCCESS
                    queued = if (ready) pending.also { pending = null } else null
                    if (!ready) pending = null
                }
                queued?.let { speak(it.first, it.second) }
            }
            tts = engine
        }
    }

    fun speak(text: String, lang: Lang) {
        synchronized(lock) {
            val engine = tts
            if (engine == null || !ready) {
                pending = text.take(600) to lang // Keep only the latest reply while initializing.
                return
            }
            val requested = Locale.forLanguageTag(lang.tag)
            val languageResult = runCatching { engine.setLanguage(requested) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
            if (languageResult < TextToSpeech.LANG_AVAILABLE) {
                runCatching { engine.setLanguage(Locale.forLanguageTag("en-IN")) }
            }
            runCatching { engine.speak(text.take(600), TextToSpeech.QUEUE_FLUSH, null, "mj") }
        }
    }

    fun stop() {
        synchronized(lock) {
            pending = null
            runCatching { tts?.stop() }
        }
    }

    fun shutdown() {
        synchronized(lock) {
            pending = null
            ready = false
            runCatching { tts?.stop() }
            runCatching { tts?.shutdown() }
            tts = null
        }
    }
}
