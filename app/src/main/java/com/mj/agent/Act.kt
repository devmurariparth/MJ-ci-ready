package com.mj.agent

import org.json.JSONObject

/** Ek agent action (model nu JSON output). */
class Act(val name: String, val args: JSONObject) {
    fun str(k: String): String = args.optString(k, "").trim()
    fun intOrNull(k: String): Int? {
        if (!args.has(k) || args.isNull(k)) return null
        return when (val value = args.opt(k)) {
            is Byte, is Short, is Int -> (value as Number).toInt()
            is Long -> value.takeIf { it in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() }?.toInt()
            is Float, is Double -> {
                val n = (value as Number).toDouble()
                n.takeIf { it.isFinite() && it % 1.0 == 0.0 && it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }?.toInt()
            }
            is String -> value.toIntOrNull()
            else -> null
        }
    }
    fun bool(k: String, def: Boolean): Boolean = args.optBoolean(k, def)
    fun brief(): String = name + " " + args.toString().take(70)
    override fun toString(): String = name + args.toString()
}

/** Action result. isFinal = kam puru thayu (ke aagal nathi vadhay). */
data class Res(val ok: Boolean, val msg: String, val isFinal: Boolean = false)
