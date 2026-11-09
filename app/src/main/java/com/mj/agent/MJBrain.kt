package com.mj.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class Msg(val fromUser: Boolean, val text: String, val kind: Int = 0) // kind 1 = agent log

enum class Mode { AUTO, CHAT, AGENT }

/** MJ nu mathu: chat, voice, agent loop, setup state. UI ane overlay banne ahi thi chale. */
object MJBrain {
    private const val MAX_STEPS = 12

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var app: MJApp
    val engine: LlmEngine by lazy { LlmEngine(app) }

    val messages = mutableStateListOf<Msg>()
    val overlayMsg = MutableStateFlow("")

    var status by mutableStateOf("")
    var busy by mutableStateOf(false)
    var listening by mutableStateOf(false)
    var modelReady by mutableStateOf(false)
    var modelLoading by mutableStateOf(false)
    var modelPresent by mutableStateOf(false)
    var modelError by mutableStateOf("")
    var importProgress by mutableIntStateOf(-1)
    var voiceLang by mutableStateOf(Lang.GU)
    var speakReplies by mutableStateOf(true)
    var mode by mutableStateOf(Mode.AUTO)
    var a11yOn by mutableStateOf(false)
    var permsOk by mutableStateOf(false)

    var requestPermissions: (() -> Unit)? = null
    private var job: Job? = null

    fun init(a: MJApp) {
        app = a
        val p = a.getSharedPreferences("mj", Context.MODE_PRIVATE)
        voiceLang = runCatching { Lang.valueOf(p.getString("lang", "GU") ?: "GU") }.getOrDefault(Lang.GU)
        mode = runCatching { Mode.valueOf(p.getString("mode", "AUTO") ?: "AUTO") }.getOrDefault(Mode.AUTO)
        speakReplies = p.getBoolean("speak", true)
        status = S.ready(voiceLang)
        refreshSetup(a)
    }

    private fun prefs() = app.getSharedPreferences("mj", Context.MODE_PRIVATE)

    fun updateVoiceLang(l: Lang) {
        voiceLang = l
        prefs().edit().putString("lang", l.name).apply()
        if (!busy && !listening) status = S.ready(l)
    }

    fun updateMode(m: Mode) {
        mode = m
        prefs().edit().putString("mode", m.name).apply()
    }

    fun updateSpeak(v: Boolean) {
        speakReplies = v
        prefs().edit().putBoolean("speak", v).apply()
        if (!v) Speaker.stop()
    }

    fun refreshSetup(ctx: Context) {
        modelPresent = engine.hasModel()
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        a11yOn = enabled.contains(ctx.packageName + "/") && enabled.contains("MJAccessibilityService")
        permsOk = listOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE, Manifest.permission.CAMERA)
            .all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }
        Apps.refresh()
    }

    // ---------- status helpers ----------

    private fun publishStatus(s: String) {
        status = s
        overlayMsg.value = s
    }

    private fun say(msg: String) {
        overlayMsg.value = msg
        scope.launch {
            delay(5000)
            if (overlayMsg.value == msg) overlayMsg.value = ""
        }
    }

    private fun reply(text: String, lang: Lang) {
        messages.add(Msg(false, text))
        say(text.take(140))
        if (speakReplies) Speaker.speak(text, lang)
    }

    // ---------- model ----------

    fun loadModelAsync() {
        scope.launch {
            if (importProgress >= 0) return@launch
            if (!modelReady && engine.hasModel()) loadModelSuspend()
            if (!busy && !listening) status = S.ready(voiceLang)
        }
    }

    private suspend fun loadModelSuspend() {
        // Never open the model while another coroutine is replacing it.
        if (importProgress >= 0) return
        if (modelLoading) {
            while (modelLoading) delay(300)
            return
        }
        if (importProgress >= 0) return
        modelLoading = true
        modelError = ""
        val old = status
        status = S.loadingModel(voiceLang)
        try {
            withContext(Dispatchers.Default) { engine.load() }
            modelReady = true
        } catch (e: Throwable) {
            modelError = e.message ?: e.javaClass.simpleName
        } finally {
            modelLoading = false
            status = if (busy) old else S.ready(voiceLang)
        }
    }

    private suspend fun ensureModel(): Boolean {
        if (modelReady) return true
        if (!engine.hasModel()) return false
        loadModelSuspend()
        return modelReady
    }

    fun importModel(uri: Uri) {
        if (busy || modelLoading || importProgress >= 0) {
            modelError = "Finish the current task or model loading before importing another model."
            return
        }
        scope.launch {
            importProgress = 0
            modelError = ""
            val tmp = File(app.filesDir, "model.task.tmp")
            try {
                withContext(Dispatchers.IO) {
                    val cr = app.contentResolver
                    var size = -1L
                    cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                        if (c.moveToFirst()) size = c.getLong(0)
                    }
                    val input = cr.openInputStream(uri) ?: throw IllegalStateException("Cannot open file")
                    input.use { inp ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(1 shl 20)
                            var total = 0L
                            var last = -1
                            while (true) {
                                val n = inp.read(buf)
                                if (n < 0) break
                                if (n == 0) continue
                                out.write(buf, 0, n)
                                total += n
                                if (size > 0) {
                                    val pct = (total * 100 / size).toInt()
                                    if (pct != last) {
                                        last = pct
                                        withContext(Dispatchers.Main.immediate) { importProgress = pct.coerceIn(0, 99) }
                                    }
                                }
                            }
                        }
                    }
                    if (!tmp.exists() || tmp.length() < 50_000_000L) {
                        throw IllegalArgumentException("This file is too small to be a supported .task model.")
                    }
                }
                // Keep the current model intact until the replacement file is fully copied
                // and passes the minimum-size check. Close native resources off the UI thread.
                withContext(Dispatchers.Default) { engine.unload() }
                modelReady = false
                withContext(Dispatchers.IO) {
                    val dst = File(app.filesDir, "model.task")
                    java.nio.file.Files.move(
                        tmp.toPath(), dst.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                    )
                }
                modelPresent = engine.hasModel()
                loadModelSuspend()
            } catch (e: Throwable) {
                modelPresent = engine.hasModel()
                modelError = e.message ?: "import failed"
            } finally {
                withContext(NonCancellable + Dispatchers.IO) { tmp.delete() }
                importProgress = -1
            }
        }
    }

    // ---------- input ----------

    fun submit(text: String) {
        val t = text.trim()
        if (t.isEmpty() || busy) return
        if (importProgress >= 0) {
            reply(S.modelImporting(voiceLang), voiceLang)
            return
        }
        job = scope.launch {
            busy = true
            val lang = detectLang(t)
            while (messages.size > 180) messages.removeAt(0)
            messages.add(Msg(true, t))
            try {
                val quick = Router.quick(t)
                if (quick != null) {
                    publishStatus(S.step(lang, 1))
                    val res = withContext(Dispatchers.Default) { Executor.run(quick, lang) }
                    reply(res.msg, lang)
                } else if (!ensureModel()) {
                    reply(if (modelError.isNotEmpty()) "Model error: $modelError" else S.noModel(lang), lang)
                } else {
                    val agent = when (mode) {
                        Mode.AUTO -> Router.looksLikeTask(t)
                        Mode.CHAT -> false
                        Mode.AGENT -> true
                    }
                    if (agent) reply(runAgent(t, lang), lang) else chat(t, lang)
                }
            } catch (e: TimeoutCancellationException) {
                reply(S.failed(lang), lang)
            } catch (e: CancellationException) {
                reply(S.stopped(lang), lang)
            } catch (e: Throwable) {
                reply("Error: ${e.message}", lang)
            } finally {
                busy = false
                status = S.ready(voiceLang)
            }
        }
    }

    fun stop() {
        job?.cancel()
        Voice.stop()
        listening = false
        Speaker.stop()
        overlayMsg.value = ""
    }

    // ---------- chat ----------

    private suspend fun chat(text: String, lang: Lang) {
        publishStatus(S.thinking(lang))
        val hist = messages.dropLast(1).filter { it.kind == 0 }.map { it.fromUser to it.text }
        messages.add(Msg(false, ""))
        val idx = messages.lastIndex
        val main = Handler(Looper.getMainLooper())
        try {
            val out = engine.generate(buildChatPrompt(hist, text, lang), 0.7f) { partial ->
                main.post { if (idx < messages.size) messages[idx] = Msg(false, partial) }
            }.trim()
            if (idx < messages.size) messages[idx] = Msg(false, out)
            say(out.take(140))
            if (speakReplies) Speaker.speak(out, lang)
        } finally {
            if (idx < messages.size && messages[idx].text.isBlank()) messages.removeAt(idx)
        }
    }

    // ---------- agent loop ----------

    private suspend fun runAgent(goal: String, lang: Lang): String {
        val hist = ArrayList<String>()
        var lastSig = ""
        var repeats = 0
        var bad = 0
        val compact = engine.maxTokens <= 1500
        val screenLimit = if (compact) 500 else 1100
        for (step in 1..MAX_STEPS) {
            publishStatus(S.step(lang, step))
            val svc = MJAccessibilityService.instance
            val screen = if (svc != null) withContext(Dispatchers.Main.immediate) { svc.snapshot(screenLimit) }
            else "(screen reading is OFF; use only call, whatsapp, alarm, timer, web, flashlight, settings, open_app, done)"
            val raw = engine.generate(AgentPrompt.build(goal, lang, hist, screen, compact), 0.2f)
            val act = AgentPrompt.parse(raw)
            if (act == null) {
                bad++
                if (bad >= 2) return S.failed(lang)
                hist.add("invalid output, reply with ONE JSON object only")
                continue
            }
            val sig = act.toString()
            if (sig == lastSig) repeats++ else { repeats = 0; lastSig = sig }
            if (repeats >= 2) return S.failed(lang)

            messages.add(Msg(false, "⚙ " + act.brief(), 1))
            val res = withContext(Dispatchers.Default) { Executor.run(act, lang) }
            hist.add(act.brief() + " -> " + res.msg.take(80))
            if (res.isFinal) return res.msg
            delay(900)
        }
        return S.failed(lang)
    }

    // ---------- voice ----------

    fun onOrbTap() = toggleListening()

    fun toggleListening() {
        if (listening) {
            Voice.stop()
            listening = false
            status = S.ready(voiceLang)
            overlayMsg.value = ""
            return
        }
        if (busy) {
            stop()
            return
        }
        val l = voiceLang
        if (ContextCompat.checkSelfPermission(app, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            say(S.micDenied(l))
            requestPermissions?.invoke()
            return
        }
        if (!Voice.available(app)) {
            say(S.noSpeech(l))
            return
        }
        Speaker.stop()
        listening = true
        publishStatus(S.listening(l))
        Voice.start(
            app, l,
            onPartial = { overlayMsg.value = it },
            onResult = { listening = false; submit(it) },
            onFail = {
                listening = false
                status = S.ready(l)
                say(S.notUnderstood(l))
            }
        )
    }
}
