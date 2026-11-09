package com.mj.agent

import org.json.JSONObject

/** Ek agent action (model nu JSON output). */
class Act(val name: String, val args: JSONObject) {
    fun str(k: String): String = args.optString(k, "").trim()
    fun intOrNull(k: String): Int? {
        if (!args.has(k)) return null
        val v = args.optInt(k, Int.MIN_VALUE)
        return if (v == Int.MIN_VALUE) null else v
    }
    fun bool(k: String, def: Boolean): Boolean = args.optBoolean(k, def)
    fun brief(): String = name + " " + args.toString().take(70)
    override fun toString(): String = name + args.toString()
}

/** Action result. isFinal = kam puru thayu (ke aagal nathi vadhay). */
data class Res(val ok: Boolean, val msg: String, val isFinal: Boolean = false)
