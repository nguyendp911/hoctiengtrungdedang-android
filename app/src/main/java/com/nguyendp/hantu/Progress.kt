package com.nguyendp.hantu

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Tiến độ học kiểu Leitner: hộp 0..5, khoảng cách ôn 0,1,2,4,7,15 ngày. */
class Progress(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private val intervals = intArrayOf(0, 1, 2, 4, 7, 15)
    private var version by mutableIntStateOf(0)

    fun today(): Long = System.currentTimeMillis() / 86_400_000L

    fun seen(id: Int): Boolean { version; return prefs.contains("d$id") }
    fun box(id: Int): Int { version; return prefs.getInt("b$id", 0) }
    fun due(id: Int): Long { version; return prefs.getLong("d$id", 0L) }
    fun isLearned(id: Int): Boolean = box(id) >= 5

    fun record(id: Int, correct: Boolean) {
        val newBox = if (correct) minOf(box(id) + 1, 5) else 0
        prefs.edit()
            .putInt("b$id", newBox)
            .putLong("d$id", today() + intervals[newBox])
            .apply()
        version++
    }

    fun setLearned(id: Int, learned: Boolean) {
        val e = prefs.edit()
        if (learned) e.putInt("b$id", 5).putLong("d$id", today() + intervals[5])
        else e.remove("b$id").remove("d$id")
        e.apply()
        version++
    }

    fun isFav(id: Int): Boolean { version; return favs().contains(id.toString()) }

    fun toggleFav(id: Int) {
        val s = favs().toMutableSet()
        if (!s.add(id.toString())) s.remove(id.toString())
        prefs.edit().putStringSet("fav", s).apply()
        version++
    }

    private fun favs(): Set<String> = prefs.getStringSet("fav", emptySet()) ?: emptySet()

    var newPerDay: Int
        get() { version; return prefs.getInt("newPerDay", 10) }
        set(v) { prefs.edit().putInt("newPerDay", v).apply(); version++ }

    fun learnedCount(words: List<Word>): Int = words.count { isLearned(it.id) }
    fun seenCount(words: List<Word>): Int = words.count { seen(it.id) }

    fun resetAll() {
        prefs.edit().clear().apply()
        version++
    }
}
