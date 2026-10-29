package com.thumbbreak.thumbbreak

/**
 * Apps we watch, and the view IDs that only exist while their short-form player is on screen.
 * These IDs are internal to Instagram/YouTube and can change with app updates —
 * verify with `adb shell uiautomator dump` or Android Studio's Layout Inspector.
 */
enum class TargetApp(val packageName: String, val viewerIds: List<String>) {
    INSTAGRAM(
        "com.instagram.android",
        listOf("clips_viewer_view_pager", "clips_video_container"),
    ),
    YOUTUBE(
        "com.google.android.youtube",
        listOf("reel_recycler", "reel_player_page_container"),
    );

    companion object {
        fun forPackage(pkg: CharSequence?): TargetApp? =
            entries.firstOrNull { it.packageName == pkg?.toString() }
    }
}
