package com.mj.agent

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Badhi apps upar tarti MJ orb. Tap = bolo. Drag = hlavo. */
class Overlay(private val svc: AccessibilityService) {
    private val wm = svc.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val density = svc.resources.displayMetrics.density
    private var box: LinearLayout? = null
    private var scope: CoroutineScope? = null

    private fun dp(v: Int): Int = (v * density).toInt()

    fun show() {
        if (box != null) return
        val orb = TextView(svc).apply {
            text = "MJ"
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF7C5CFF.toInt(), 0xFF00D4FF.toInt())
            ).apply { shape = GradientDrawable.OVAL }
            elevation = dp(6).toFloat()
        }
        orb.setOnClickListener { MJBrain.onOrbTap() }
        val label = TextView(svc).apply {
            setTextColor(Color.WHITE)
            textSize = 13f
            maxWidth = dp(240)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = GradientDrawable().apply {
                setColor(0xE6151B2B.toInt())
                cornerRadius = dp(14).toFloat()
            }
            visibility = View.GONE
        }
        val container = LinearLayout(svc).apply {
            orientation = LinearLayout.VERTICAL
            addView(orb, LinearLayout.LayoutParams(dp(54), dp(54)))
            addView(
                label,
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                    .apply { topMargin = dp(6) }
            )
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(8)
            y = dp(240)
        }

        var sx = 0f
        var sy = 0f
        var ox = 0
        var oy = 0
        var moved = false
        orb.setOnTouchListener { v, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    sx = e.rawX; sy = e.rawY; ox = lp.x; oy = lp.y; moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - sx).toInt()
                    val dy = (e.rawY - sy).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) moved = true
                    if (moved) {
                        val display = svc.resources.displayMetrics
                        // Keep the orb reachable; dragging beyond an edge must not hide it.
                        lp.x = (ox + dx).coerceIn(0, (display.widthPixels - dp(54)).coerceAtLeast(0))
                        lp.y = (oy + dy).coerceIn(0, (display.heightPixels - dp(54)).coerceAtLeast(0))
                        try { wm.updateViewLayout(container, lp) } catch (_: Throwable) {}
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) v.performClick()
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(container, lp)
        } catch (e: Throwable) {
            return
        }
        box = container

        val sc = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        scope = sc
        sc.launch {
            MJBrain.overlayMsg.collect { m ->
                label.text = m
                label.visibility = if (m.isBlank()) View.GONE else View.VISIBLE
                val s = if (m.isBlank()) 1f else 1.15f
                orb.animate().scaleX(s).scaleY(s).setDuration(200).start()
            }
        }
    }

    fun hide() {
        scope?.cancel()
        scope = null
        val b = box ?: return
        box = null
        try { wm.removeView(b) } catch (_: Throwable) {}
    }
}
