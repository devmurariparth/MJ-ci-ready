package com.mj.agent

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/** Speech-to-text (Android built-in recognizer, free, API key vagar). Main thread par j vapro. */
object Voice {
    private var rec: SpeechRecognizer? = null
    private var sessionId = 0
    private val main = Handler(Looper.getMainLooper())
    private var timeout: Runnable? = null

    fun available(ctx: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun start(
        ctx: Context,
        lang: Lang,
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onFail: (Int) -> Unit
    ) {
        stop()
        val currentSession = ++sessionId
        var delivered = false
        fun failOnce(code: Int) {
            if (currentSession != sessionId || delivered) return
            delivered = true
            timeout?.let(main::removeCallbacks)
            timeout = null
            onFail(code)
        }
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(ctx.applicationContext)
        } catch (_: RuntimeException) {
            failOnce(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        rec = r
        val timeoutTask = Runnable {
            if (currentSession == sessionId && !delivered) {
                failOnce(SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                stop()
            }
        }
        timeout = timeoutTask
        main.postDelayed(timeoutTask, 20_000L)
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (currentSession == sessionId && !delivered && !t.isNullOrBlank()) onPartial(t)
            }

            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (currentSession != sessionId || delivered) return
                delivered = true
                timeout?.let(main::removeCallbacks)
                timeout = null
                main.post { if (currentSession == sessionId) stop() }
                if (t.isNullOrBlank()) onFail(SpeechRecognizer.ERROR_NO_MATCH) else onResult(t)
            }

            override fun onError(error: Int) {
                failOnce(error)
                Handler(Looper.getMainLooper()).post { if (currentSession == sessionId) stop() }
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }
        try {
            r.startListening(i)
        } catch (_: RuntimeException) {
            failOnce(SpeechRecognizer.ERROR_CLIENT)
            stop()
        }
    }

    fun stop() {
        timeout?.let(main::removeCallbacks)
        timeout = null
        sessionId++
        try { rec?.destroy() } catch (_: Throwable) {}
        rec = null
    }
}
