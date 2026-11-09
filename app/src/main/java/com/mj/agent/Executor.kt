package com.mj.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Model ni action ne asli kam ma badle che. */
object Executor {
    suspend fun run(act: Act, lang: Lang): Res {
        val svc = MJAccessibilityService.instance
        val noA11y = Res(false, S.needAccessibility(lang), true)
        return when (act.name) {
            "open_app", "open" -> Device.openApp(act.str("app").ifBlank { act.str("name") }, lang)
            "call", "dial" -> Device.call(act.str("to").ifBlank { act.str("name") }, lang)
            "play_music", "play_song", "music" -> Device.playMusic(
                act.str("query").ifBlank { act.str("song") }, act.str("provider"), lang
            )
            "whatsapp", "send_whatsapp", "message" ->
                Device.whatsapp(act.str("to").ifBlank { act.str("name") }, act.str("text").ifBlank { act.str("msg") }, lang)
            "alarm" -> Device.alarm(act.intOrNull("hour") ?: -1, act.intOrNull("minute") ?: 0, act.str("label"), lang)
            "timer" -> Device.timer(act.intOrNull("seconds") ?: 0, lang)
            "web", "search" -> Device.web(act.str("q").ifBlank { act.str("query") }, lang)
            "settings" -> Device.openSettings(act.str("page"), lang)
            "flashlight", "torch" -> Device.flashlight(act.bool("on", true), lang)
            "click", "tap" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) {
                    val id = act.intOrNull("id")
                    if (id != null) svc.clickIndex(id) else svc.clickText(act.str("text"))
                }
                Res(ok, if (ok) "clicked" else "click failed: element not found")
            }
            "type" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) { svc.typeText(act.str("text"), act.intOrNull("id")) }
                Res(ok, if (ok) "typed" else "type failed: no input field")
            }
            "enter", "submit" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) { svc.pressEnter() }
                Res(ok, if (ok) "enter" else "enter failed")
            }
            "scroll" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) { svc.scroll(act.str("dir").lowercase() != "up") }
                Res(ok, if (ok) "scrolled" else "scroll failed")
            }
            "back" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) { svc.back() }
                Res(ok, if (ok) "back" else "back failed")
            }
            "home" -> {
                if (svc == null) return noA11y
                val ok = withContext(Dispatchers.Main.immediate) { svc.home() }
                Res(ok, if (ok) "home" else "home failed")
            }
            "wait" -> { delay(1200); Res(true, "waited") }
            "done", "answer", "reply", "finish" ->
                Res(true, act.str("msg").ifBlank { act.str("text") }.ifBlank { S.done(lang) }, true)
            else -> Res(false, "unknown action: ${act.name}")
        }
    }
}
