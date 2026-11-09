package com.mj.agent

import android.content.Context
import android.content.Intent

/** Installed apps shodhva ane Gujarati/Hindi/English naam thi match karva. */
object Apps {
    data class AppInfo(val label: String, val pkg: String)

    private var cache: List<AppInfo>? = null

    fun installed(ctx: Context = MJApp.instance): List<AppInfo> {
        cache?.let { return it }
        val pm = ctx.packageManager
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val list = pm.queryIntentActivities(i, 0)
            .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .distinctBy { it.pkg }
        cache = list
        return list
    }

    fun refresh() { cache = null }

    private val table: List<Triple<String, List<String>, List<String>>> = listOf(
        Triple("whatsapp", listOf("whatsapp", "whats app", "વોટ્સએપ", "વ્હોટ્સએપ", "વૉટ્સએપ", "वॉट्सऐप", "व्हाट्सएप", "व्हाट्सऐप", "वाट्सएप"), listOf("com.whatsapp", "com.whatsapp.w4b")),
        Triple("youtube music", listOf("youtube music", "yt music", "યુટ્યુબ મ્યુઝિક", "यूट्यूब म्यूजिक", "यूट्यूब म्यूज़िक"), listOf("com.google.android.apps.youtube.music")),
        Triple("youtube", listOf("youtube", "you tube", "યુટ્યુબ", "यूट्यूब"), listOf("com.google.android.youtube")),
        Triple("chrome", listOf("chrome", "browser", "ક્રોમ", "બ્રાઉઝર", "क्रोम", "ब्राउज़र"), listOf("com.android.chrome")),
        Triple("camera", listOf("camera", "કેમેરા", "कैमरा"), listOf("com.google.android.GoogleCamera", "com.android.camera2", "com.sec.android.app.camera", "com.miui.camera")),
        Triple("settings", listOf("settings", "setting", "સેટિંગ્સ", "સેટિંગ", "सेटिंग्स", "सेटिंग"), listOf("com.android.settings")),
        Triple("gallery", listOf("gallery", "photos", "ગેલેરી", "ફોટોઝ", "गैलरी", "फोटो", "फोटोज"), listOf("com.google.android.apps.photos", "com.sec.android.gallery3d", "com.miui.gallery")),
        Triple("phone", listOf("phone", "dialer", "ફોન", "डायलर", "फोन"), listOf("com.google.android.dialer", "com.android.dialer", "com.samsung.android.dialer")),
        Triple("maps", listOf("maps", "google maps", "મેપ્સ", "નકશો", "मैप्स", "नक्शा"), listOf("com.google.android.apps.maps")),
        Triple("instagram", listOf("instagram", "insta", "ઇન્સ્ટાગ્રામ", "इंस्टाग्राम"), listOf("com.instagram.android")),
        Triple("facebook", listOf("facebook", "ફેસબુક", "फेसबुक"), listOf("com.facebook.katana")),
        Triple("gmail", listOf("gmail", "જીમેલ", "जीमेल"), listOf("com.google.android.gm")),
        Triple("play store", listOf("play store", "playstore", "પ્લે સ્ટોર", "प्ले स्टोर"), listOf("com.android.vending")),
        Triple("calculator", listOf("calculator", "કેલ્ક્યુલેટર", "कैलकुलेटर"), listOf("com.google.android.calculator", "com.android.calculator2", "com.sec.android.app.popupcalculator", "com.miui.calculator")),
        Triple("clock", listOf("clock", "ઘડિયાળ", "घड़ी"), listOf("com.google.android.deskclock", "com.android.deskclock")),
        Triple("telegram", listOf("telegram", "ટેલિગ્રામ", "टेलीग्राम"), listOf("org.telegram.messenger")),
        Triple("spotify", listOf("spotify", "સ્પોટિફાય", "स्पॉटिफाई"), listOf("com.spotify.music")),
        Triple("phonepe", listOf("phonepe", "phone pe", "ફોનપે", "फोनपे"), listOf("com.phonepe.app")),
        Triple("paytm", listOf("paytm", "પેટીએમ", "पेटीएम"), listOf("net.one97.paytm")),
        Triple("pay", listOf("gpay", "google pay", "ગૂગલ પે", "गूगल पे"), listOf("com.google.android.apps.nbu.paisa.user"))
    )

    private fun norm(s: String): String =
        s.lowercase().replace(Regex("[^\\p{L}\\p{M}\\p{N} ]"), "").replace(Regex("\\s+"), " ").trim()

    fun hasPkg(pkg: String): Boolean = installed().any { it.pkg == pkg }

    fun resolve(query: String, strict: Boolean = false): AppInfo? {
        val q = norm(query)
        if (q.isBlank()) return null
        val apps = installed()
        for (entry in table) {
            val canon = entry.first
            val aliases = entry.second.map { norm(it) }
            val hit = aliases.any { it == q } || (!strict && aliases.any { it.length >= 3 && q.contains(it) })
            if (hit) {
                for (p in entry.third) {
                    val found = apps.firstOrNull { it.pkg == p }
                    if (found != null) return found
                }
                val byLabel = apps.firstOrNull { norm(it.label).contains(canon) }
                if (byLabel != null) return byLabel
            }
        }
        apps.firstOrNull { norm(it.label) == q }?.let { return it }
        if (q.length >= 3) apps.firstOrNull { norm(it.label).startsWith(q) }?.let { return it }
        if (!strict) {
            if (q.length >= 3) apps.firstOrNull { norm(it.label).contains(q) }?.let { return it }
            apps.firstOrNull { val l = norm(it.label); l.length >= 4 && q.contains(l) }?.let { return it }
        }
        return null
    }
}
