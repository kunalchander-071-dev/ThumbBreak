package com.thumbbreak.thumbbreak

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

/**
 * Watches Instagram and YouTube. While the Reels/Shorts player is visible it counts each
 * swipe to a new item and shows a live counter badge; once the daily limit is hit it
 * presses Back to leave the player. The rest of each app (feed, DMs, search, long videos)
 * is untouched.
 */
class ShortsBlockerService : AccessibilityService() {

    private lateinit var store: Store
    private lateinit var badge: CounterBadge

    private var inViewer = false
    private var lastCountAt = 0L
    private var lastCheckAt = 0L
    private var lastBlockAt = 0L

    override fun onServiceConnected() {
        store = Store(this)
        badge = CounterBadge(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val app = TargetApp.forPackage(event.packageName)
        if (app == null) {
            // Switched to another app (home screen, etc.) — drop the badge.
            // Our own badge/toast also fire events; ignore those.
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                event.packageName != packageName
            ) leaveViewer()
            return
        }
        val now = SystemClock.uptimeMillis()

        // Content-changed events fire constantly; don't walk the view tree for every one.
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            now - lastCheckAt < CHECK_THROTTLE_MS
        ) return
        lastCheckAt = now

        val root = rootInActiveWindow ?: return
        if (!isViewerVisible(root, app)) {
            leaveViewer()
            return
        }
        val justEntered = !inViewer
        inViewer = true

        if (store.isLimitReached()) {
            block(now)
            return
        }

        val swiped = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        if ((justEntered || swiped) && now - lastCountAt > SWIPE_DEBOUNCE_MS) {
            lastCountAt = now
            store.increment(app)
        }
        badge.show(store.total(), store.limit)
        if (store.isLimitReached()) block(now)
    }

    private fun isViewerVisible(root: AccessibilityNodeInfo, app: TargetApp): Boolean =
        app.viewerIds.any { id ->
            root.findAccessibilityNodeInfosByViewId("${app.packageName}:id/$id")
                .any { it.isVisibleToUser }
        }

    private fun leaveViewer() {
        inViewer = false
        badge.hide()
    }

    private fun block(now: Long) {
        // One Back per block window, so we don't back out of the whole app.
        if (now - lastBlockAt < BLOCK_COOLDOWN_MS) return
        lastBlockAt = now
        leaveViewer()
        performGlobalAction(GLOBAL_ACTION_BACK)
        Toast.makeText(this, "Daily limit reached — take a break", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        badge.hide()
        super.onDestroy()
    }

    companion object {
        private const val SWIPE_DEBOUNCE_MS = 800L
        private const val CHECK_THROTTLE_MS = 300L
        private const val BLOCK_COOLDOWN_MS = 1500L
    }
}
