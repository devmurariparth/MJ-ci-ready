package com.mj.agent

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Intent aadhaarit device actions: call, whatsapp, alarm, timer, web, torch, settings. */
object Device {
    private fun ctx(): Context = MJAccessibilityService.instance ?: MJApp.instance

    private fun launch(i: Intent): Boolean = try {
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx().startActivity(i)
        true
    } catch (e: Exception) {
        false
    }

    fun openApp(name: String, lang: Lang): Res {
        val app = Apps.resolve(name) ?: return Res(false, S.appNotFound(lang, name))
        val i = ctx().packageManager.getLaunchIntentForPackage(app.pkg)
            ?: return Res(false, S.appNotFound(lang, name))
        return if (launch(i)) Res(true, S.opened(lang, app.label)) else Res(false, S.failed(lang), true)
    }

    fun call(to: String, lang: Lang): Res {
        val c = ctx()
        val hit = Contacts.find(c, to) ?: return Res(false, S.contactNotFound(lang, to), true)
        val uri = Uri.parse("tel:" + Uri.encode(hit.second))
        val granted = ContextCompat.checkSelfPermission(c, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val i = Intent(if (granted) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri)
        return if (launch(i)) Res(true, S.calling(lang, hit.first), true) else Res(false, S.failed(lang), true)
    }

    suspend fun whatsapp(to: String, text: String, lang: Lang): Res {
        val c = ctx()
        val hit = Contacts.find(c, to) ?: return Res(false, S.contactNotFound(lang, to), true)
        val pkg = listOf("com.whatsapp", "com.whatsapp.w4b").firstOrNull { Apps.hasPkg(it) }
            ?: return Res(false, S.appNotFound(lang, "WhatsApp"), true)
        val num = Contacts.normalizeForWhatsApp(hit.second)
        val uri = Uri.parse("https://wa.me/$num?text=" + Uri.encode(text))
        val i = Intent(Intent.ACTION_VIEW, uri).setPackage(pkg)
        if (!launch(i)) return Res(false, S.failed(lang), true)
        val svc = MJAccessibilityService.instance ?: return Res(true, S.waOpened(lang, hit.first), true)
        for (n in 1..12) {
            delay(800)
            val didSend = withContext(Dispatchers.Main.immediate) { svc.clickSend() }
            if (didSend) {
                delay(600)
                return Res(true, S.waSent(lang, hit.first), true)
            }
        }
        return Res(true, S.waOpened(lang, hit.first), true)
    }

    fun alarm(hour: Int, minute: Int, label: String, lang: Lang): Res {
        if (hour !in 0..23 || minute !in 0..59) return Res(false, S.failed(lang), true)
        val i = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, label)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return if (launch(i)) Res(true, S.alarmSet(lang, hour, minute), true) else Res(false, S.failed(lang), true)
    }

    fun timer(seconds: Int, lang: Lang): Res {
        if (seconds <= 0) return Res(false, S.failed(lang), true)
        val i = Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        return if (launch(i)) Res(true, S.timerSet(lang, seconds), true) else Res(false, S.failed(lang), true)
    }

    fun web(q: String, lang: Lang): Res {
        if (q.isBlank()) return Res(false, S.failed(lang), true)
        val looksUrl = q.startsWith("http://") || q.startsWith("https://") ||
            (!q.contains(" ") && q.contains(".") && q.length > 3)
        val url = if (looksUrl) (if (q.startsWith("http")) q else "https://$q")
        else "https://www.google.com/search?q=" + Uri.encode(q)
        return if (launch(Intent(Intent.ACTION_VIEW, Uri.parse(url)))) Res(true, S.web(lang), true)
        else Res(false, S.failed(lang), true)
    }

    fun flashlight(on: Boolean, lang: Lang): Res {
        val context = ctx()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return Res(false, S.cameraPermission(lang), true)
        }
        return try {
            val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull {
                cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) {
                Res(false, S.failed(lang), true)
            } else {
                cm.setTorchMode(id, on)
                Res(true, S.torch(lang, on), true)
            }
        } catch (e: Exception) {
            Res(false, S.failed(lang), true)
        }
    }

    fun openSettings(page: String, lang: Lang): Res {
        val action = when (page.lowercase()) {
            "wifi", "wi-fi" -> Settings.ACTION_WIFI_SETTINGS
            "bluetooth" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "display", "brightness" -> Settings.ACTION_DISPLAY_SETTINGS
            "sound", "volume" -> Settings.ACTION_SOUND_SETTINGS
            "battery" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return if (launch(Intent(action))) Res(true, S.settingsOpened(lang)) else Res(false, S.failed(lang), true)
    }
}
