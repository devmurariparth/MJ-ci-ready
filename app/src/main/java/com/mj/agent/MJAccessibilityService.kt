package com.mj.agent

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/** Phone ni screen vanche ane tap/type/scroll kare. */
class MJAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: MJAccessibilityService? = null
    }

    @Volatile private var nodes: List<AccessibilityNodeInfo> = emptyList()
    private var overlay: Overlay? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        overlay = Overlay(this).also { it.show() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        overlay?.hide()
        overlay = null
        instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        overlay?.hide()
        instance = null
        super.onDestroy()
    }

    // ---------- Screen reading ----------

    fun snapshot(limit: Int = 1100): String {
        val root = rootInActiveWindow ?: return "(screen not available)"
        val list = ArrayList<AccessibilityNodeInfo>()
        val sb = StringBuilder()
        sb.append("APP: ").append(root.packageName).append("\n")
        val scrollable = booleanArrayOf(false)
        walk(root, list, sb, 0, scrollable)
        if (scrollable[0]) sb.append("(screen is scrollable)\n")
        nodes = list
        return if (sb.length > limit) sb.substring(0, limit) else sb.toString()
    }

    private fun walk(
        n: AccessibilityNodeInfo?, out: ArrayList<AccessibilityNodeInfo>,
        sb: StringBuilder, depth: Int, scrollable: BooleanArray
    ) {
        if (n == null || depth > 30 || out.size >= 45) return
        if (!n.isVisibleToUser) return
        if (n.isScrollable) scrollable[0] = true
        var label = (n.text ?: n.contentDescription ?: n.hintText)?.toString()
            ?.replace("\n", " ")?.trim().orEmpty()
        val isInput = n.isEditable
        val isBtn = n.isClickable
        if (label.isEmpty() && isBtn && n.childCount == 0) {
            label = n.viewIdResourceName?.substringAfterLast('/').orEmpty()
        }
        if (label.isNotEmpty() || isInput) {
            val tag = if (isInput) "input" else if (isBtn) "btn" else "text"
            val shown = if (label.length > 40) label.substring(0, 40) else label
            sb.append('[').append(out.size).append("] ").append(tag).append(" \"").append(shown).append("\"\n")
            out.add(n)
        }
        for (i in 0 until n.childCount) {
            walk(n.getChild(i), out, sb, depth + 1, scrollable)
        }
    }

    // ---------- Actions ----------

    private fun clickableAncestor(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        var n = node
        var guard = 0
        while (n != null && !n.isClickable && guard < 8) {
            n = n.parent
            guard++
        }
        return if (n != null && n.isClickable) n else null
    }

    fun clickNode(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        val target = clickableAncestor(node)
        if (target != null && target.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        val r = Rect()
        node.getBoundsInScreen(r)
        if (r.width() <= 0 || r.height() <= 0) return false
        return tap(r.exactCenterX(), r.exactCenterY())
    }

    fun clickIndex(i: Int): Boolean = clickNode(nodes.getOrNull(i))

    fun clickText(text: String): Boolean {
        if (text.isBlank()) return false
        val root = rootInActiveWindow ?: return false
        val hit = root.findAccessibilityNodeInfosByText(text).firstOrNull() ?: return false
        return clickNode(hit)
    }

    /** WhatsApp ma Send button dabave. */
    fun clickSend(): Boolean {
        val root = rootInActiveWindow ?: return false
        for (id in listOf("com.whatsapp:id/send", "com.whatsapp.w4b:id/send")) {
            val byId = root.findAccessibilityNodeInfosByViewId(id)
            if (!byId.isNullOrEmpty()) return clickNode(byId[0])
        }
        for (label in listOf("Send", "મોકલો", "भेजें")) {
            val match = root.findAccessibilityNodeInfosByText(label).firstOrNull()
            if (match != null && clickNode(match)) return true
        }
        return false
    }

    fun typeText(text: String, id: Int?): Boolean {
        var target: AccessibilityNodeInfo? = id?.let { nodes.getOrNull(it) }
        if (target == null || !target.isEditable) target = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (target == null || !target.isEditable) target = nodes.firstOrNull { it.isEditable }
        if (target == null) return false
        target.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return target.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    fun pressEnter(): Boolean {
        if (Build.VERSION.SDK_INT >= 30) {
            val f = rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
            if (f != null) return f.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_IME_ENTER.id)
        }
        return false
    }

    /** Starts Android's secure system screenshot flow; callback is asynchronous. */
    fun takeScreenshot(): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        return try {
            takeScreenshot(
                AccessibilityService.SCREENSHOT_TYPE_FULLSCREEN,
                mainExecutor,
                object : AccessibilityService.TakeScreenshotCallback {
                    override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                        // Screenshot result is intentionally not persisted or uploaded by MJ.
                        result.hardwareBuffer.close()
                    }
                    override fun onFailure(errorCode: Int) {}
                }
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun back(): Boolean = performGlobalAction(GLOBAL_ACTION_BACK)
    fun home(): Boolean = performGlobalAction(GLOBAL_ACTION_HOME)

    fun tap(x: Float, y: Float): Boolean {
        val p = Path().apply { moveTo(x, y) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 60))
            .build()
        return dispatchGesture(g, null, null)
    }

    fun scroll(down: Boolean): Boolean {
        val dm = resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val from = if (down) h * 0.72f else h * 0.28f
        val to = if (down) h * 0.28f else h * 0.72f
        val p = Path().apply { moveTo(w / 2f, from); lineTo(w / 2f, to) }
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(p, 0, 350))
            .build()
        return dispatchGesture(g, null, null)
    }
}
