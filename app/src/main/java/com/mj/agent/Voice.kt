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

    fun available(ctx: Context): Boolean = SpeechRecognizer.isRecognitionAvailable(ctx)

    fun start(
        ctx: Context,
        lang: Lang,
        onPartial: (String) -> Unit,
        onResult: (String) -> Unit,
        onFail: (Int) -> Unit
    ) {
        stop()
        val r = try {
            SpeechRecognizer.createSpeechRecognizer(ctx.applicationContext)
        } catch (_: RuntimeException) {
            onFail(SpeechRecognizer.ERROR_CLIENT)
            return
        }
        rec = r
        r.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val t = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!t.isNullOrBlank()) onPartial(t)
            }

            override fun onResults(results: Bundle?) {
                val t = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                Handler(Looper.getMainLooper()).post { stop() }
                if (t.isNullOrBlank()) onFail(SpeechRecognizer.ERROR_NO_MATCH) else onResult(t)
            }

            override fun onError(error: Int) {
                Handler(Looper.getMainLooper()).post { stop() }
                onFail(error)
            }
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang.tag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            r.startListening(i)
        } catch (_: RuntimeException) {
            stop()
            onFail(SpeechRecognizer.ERROR_CLIENT)
        }
    }

    fun stop() {
        try { rec?.destroy() } catch (_: Throwable) {}
        rec = null
    }
}
