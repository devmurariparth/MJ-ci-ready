package com.mj.agent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

/** On-device LLM (MediaPipe). Koi API nahi, internet nahi. */
class LlmEngine(private val ctx: Context) {
    @Volatile private var llm: LlmInference? = null
    private val mutex = Mutex()
    @Volatile var maxTokens: Int = 2048
        private set

    fun modelFile(): File = File(ctx.filesDir, "model.task")

    fun hasModel(): Boolean = modelFile().let { it.exists() && it.length() > 50_000_000L }
    fun isLoaded(): Boolean = llm != null

    /** Blocking. Background thread ma bolavo. */
    fun load() {
        if (llm != null) return
        var lastError: Throwable? = null
        for (tokens in intArrayOf(2048, 1280)) {
            try {
                val opts = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(modelFile().absolutePath)
                    .setMaxTokens(tokens)
                    .build()
                llm = LlmInference.createFromOptions(ctx, opts)
                maxTokens = tokens
                return
            } catch (e: Throwable) {
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Model load failed")
    }

    fun unload() {
        try { llm?.close() } catch (_: Throwable) {}
        llm = null
    }

    /** Ek prompt -> poori reply. Nava session thi dar vakhat saaf context. */
    suspend fun generate(
        prompt: String,
        temperature: Float = 0.7f,
        onPartial: ((String) -> Unit)? = null
    ): String = mutex.withLock {
        withContext(Dispatchers.Default) {
            val model = llm ?: throw IllegalStateException("Model loaded nathi")
            val sopts = LlmInferenceSession.LlmInferenceSessionOptions.builder()
                .setTopK(40)
                .setTemperature(temperature)
                .setRandomSeed(7)
                .build()
            val session = LlmInferenceSession.createFromOptions(model, sopts)
            try {
                session.addQueryChunk(prompt)
                val done = CompletableDeferred<String>()
                val sb = StringBuilder()
                session.generateResponseAsync { part, finished ->
                    sb.append(part)
                    onPartial?.invoke(sb.toString())
                    if (finished) done.complete(sb.toString())
                }
                try {
                    withTimeout(180_000L) { done.await() }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    // MediaPipe exposes a native async-cancel operation. Wait briefly for its
                    // completion callback before closing the native session.
                    withContext(NonCancellable) {
                        // Called via reflection so the build cannot break if a MediaPipe
                        // version does not ship this method.
                        runCatching { session.javaClass.getMethod("cancelGenerateResponseAsync").invoke(session) }
                        withTimeoutOrNull(5_000L) { done.await() }
                    }
                    throw cancelled
                }
            } finally {
                try { session.close() } catch (_: Throwable) {}
            }
        }
    }
}
