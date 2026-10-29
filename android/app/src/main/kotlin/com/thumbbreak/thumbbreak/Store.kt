package com.thumbbreak.thumbbreak

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Today's counts + the daily limit, persisted in SharedPreferences. Resets at local midnight. */
class Store(context: Context) {
    val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun rollover() {
        val today = today()
        if (prefs.getString(KEY_DATE, null) != today) {
            prefs.edit()
                .putString(KEY_DATE, today)
                .putInt(KEY_INSTAGRAM, 0)
                .putInt(KEY_YOUTUBE, 0)
                .apply()
        }
    }

    var limit: Int
        get() = prefs.getInt(KEY_LIMIT, DEFAULT_LIMIT)
        set(value) = prefs.edit().putInt(KEY_LIMIT, value).apply()

    fun total(): Int {
        rollover()
        return prefs.getInt(KEY_INSTAGRAM, 0) + prefs.getInt(KEY_YOUTUBE, 0)
    }

    fun isLimitReached(): Boolean = total() >= limit

    fun increment(app: TargetApp) {
        rollover()
        val key = if (app == TargetApp.INSTAGRAM) KEY_INSTAGRAM else KEY_YOUTUBE
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    // Diagnostics live in a separate file so writing them doesn't fire the live-count listener.
    private val debugPrefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE)

    fun saveViewIds(app: TargetApp, ids: String) =
        debugPrefs.edit().putString(app.name, ids).apply()

    fun viewIds(): Map<String, String> =
        TargetApp.entries.associate { it.name to (debugPrefs.getString(it.name, null) ?: "") }

    fun toMap(): Map<String, Any> {
        rollover()
        return mapOf(
            "instagram" to prefs.getInt(KEY_INSTAGRAM, 0),
            "youtube" to prefs.getInt(KEY_YOUTUBE, 0),
            "limit" to limit,
        )
    }

    companion object {
        private const val PREFS = "thumbbreak"
        private const val DEBUG_PREFS = "thumbbreak_debug"
        private const val KEY_DATE = "date"
        private const val KEY_INSTAGRAM = "count_instagram"
        private const val KEY_YOUTUBE = "count_youtube"
        private const val KEY_LIMIT = "limit"
        private const val DEFAULT_LIMIT = 20
    }
}
