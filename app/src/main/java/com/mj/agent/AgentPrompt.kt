package com.mj.agent

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AgentPrompt {
    private val SYS = """
You are MJ, an AI agent that controls an Android phone. Reply with EXACTLY ONE JSON object and nothing else.

Actions:
{"a":"open_app","app":"WhatsApp"}
{"a":"call","to":"name or number"}
{"a":"whatsapp","to":"name or number","text":"message"}
{"a":"alarm","hour":7,"minute":30}
{"a":"timer","seconds":300}
{"a":"web","q":"search text or URL"}
{"a":"settings","page":"wifi|bluetooth|display|main"}
{"a":"flashlight","on":true}
{"a":"click","id":3}
{"a":"type","text":"hello"}
{"a":"enter"}
{"a":"scroll","dir":"down"}
{"a":"back"}
{"a":"home"}
{"a":"wait"}
{"a":"done","msg":"final reply"}

Rules:
1. Prefer call, whatsapp, alarm, timer, web, flashlight, settings, open_app. Use click/type only when needed.
2. "id" is the [number] of an element in SCREEN.
3. After each action you get the new SCREEN. When the task is finished, or if it is only a question, output done.
4. The "msg" and any text typed for the user must be in the user's language: LANG.
""".trimIndent()

    private val EXAMPLES = """
Examples:
TASK: WhatsApp par Rahul ne hu aavu chu moklo
{"a":"whatsapp","to":"Rahul","text":"hu aavu chu"}
TASK: set alarm for 6:45 am
{"a":"alarm","hour":6,"minute":45}
TASK: what is 5 plus 7
{"a":"done","msg":"12"}
""".trimIndent()

    fun build(goal: String, lang: Lang, hist: List<String>, screen: String, compact: Boolean): String {
        val now = SimpleDateFormat("EEEE, d MMM yyyy, HH:mm", Locale.ENGLISH).format(Date())
        val sb = StringBuilder()
        sb.append("<start_of_turn>user\n")
        sb.append(SYS.replace("LANG", lang.instruction.removePrefix("Reply ONLY in ").removeSuffix(".")))
        sb.append("\n\n")
        if (!compact) sb.append(EXAMPLES).append("\n\n")
        sb.append("NOW: ").append(now).append("\n")
        sb.append("TASK: ").append(goal).append("\n")
        if (hist.isNotEmpty()) {
            sb.append("HISTORY:\n")
            hist.takeLast(if (compact) 3 else 5).forEachIndexed { i, h -> sb.append("- ").append(h).append("\n") }
        }
        sb.append("SCREEN:\n").append(screen).append("\n")
        sb.append("Next JSON action:<end_of_turn>\n<start_of_turn>model\n")
        return sb.toString()
    }

    /** Model output ma thi pehlo poora JSON object kadhi Act banave. */
    fun parse(raw: String): Act? {
        val s = raw.indexOf('{')
        if (s < 0) return null
        var depth = 0
        var inStr = false
        var esc = false
        for (i in s until raw.length) {
            val c = raw[i]
            if (inStr) {
                if (esc) esc = false
                else if (c == '\\') esc = true
                else if (c == '"') inStr = false
            } else {
                when (c) {
                    '"' -> inStr = true
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) return toAct(raw.substring(s, i + 1))
                    }
                }
            }
        }
        return null
    }

    private fun toAct(json: String): Act? = try {
        val o = JSONObject(json)
        val a = o.optString("a", o.optString("action", "")).trim().lowercase()
        if (a.isEmpty()) null else Act(a, o)
    } catch (e: Exception) {
        null
    }
}
