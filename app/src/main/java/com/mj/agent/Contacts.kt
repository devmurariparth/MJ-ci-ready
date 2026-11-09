package com.mj.agent

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object Contacts {
    fun hasReadPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun isNumber(s: String): Boolean {
        val d = s.replace(Regex("[\\s\\-()+]"), "")
        return d.length >= 6 && d.all { it.isDigit() }
    }

    /** Returns a phone number only when the match is unambiguous. */
    fun find(ctx: Context, query: String): Pair<String, String>? {
        val q = query.trim()
        if (q.isEmpty()) return null
        if (isNumber(q)) return q to q
        if (!hasReadPermission(ctx)) return null

        val cursor = ctx.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
            arrayOf("%$q%"),
            null
        ) ?: return null

        val matches = cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    val name = c.getString(0)?.trim().orEmpty()
                    val number = c.getString(1)?.trim().orEmpty()
                    if (name.isNotEmpty() && number.isNotEmpty()) add(name to number)
                }
            }
        }.distinctBy { it.first.lowercase() + "|" + it.second.filter(Char::isDigit) }

        val normalizedQuery = normalizeName(q)
        if (normalizedQuery.isEmpty()) return null
        val exact = matches.filter { normalizeName(it.first) == normalizedQuery }
        if (exact.size == 1) return exact.first()
        if (exact.size > 1) return null

        val prefix = matches.filter { normalizeName(it.first).startsWith(normalizedQuery) }
        if (prefix.size == 1) return prefix.first()
        if (prefix.size > 1) return null

        val partial = matches.filter { normalizeName(it.first).contains(normalizedQuery) }
        return partial.singleOrNull()
    }

    private fun normalizeName(value: String): String =
        value.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")

    fun normalizeForWhatsApp(raw: String): String {
        val d = raw.filter { it.isDigit() }
        return when {
            d.length == 10 -> "91$d"
            d.length == 11 && d.startsWith("0") -> "91" + d.drop(1)
            else -> d
        }
    }
}
