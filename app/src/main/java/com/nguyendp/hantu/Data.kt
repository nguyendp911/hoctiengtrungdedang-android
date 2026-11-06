package com.nguyendp.hantu

import android.content.Context
import java.text.Normalizer
import org.json.JSONArray

data class Word(
    val id: Int,
    val topic: String,
    val hanzi: String,
    val pinyin: String,
    val readingVi: String,
    val hanViet: String,
    val meaningVi: String,
    val hsk: Int?,
    val unitNo: Int?,
    val unit: String?,
    val exZh: String?,
    val exPy: String?,
    val exVi: String?,
) {
    /** Cấp HSK 1..7 (7 = HSK 7–9); từ chủ đề không thuộc cấp nào thì null. */
    val level: Int?
        get() = hsk ?: when {
            topic.startsWith("HSK 7") -> 7
            topic.startsWith("HSK ") -> topic.drop(4).toIntOrNull()
            else -> null
        }

    val searchText: String by lazy { fold("$hanzi $pinyin $hanViet $meaningVi $readingVi") }
}

fun fold(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace('đ', 'd')
        .replace('Đ', 'D')
        .lowercase()

fun levelName(level: Int): String = if (level == 7) "HSK 7–9" else "HSK $level"

object WordRepo {
    fun load(ctx: Context): List<Word> {
        val text = ctx.assets.open("phrases.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val arr = JSONArray(text)
        val out = ArrayList<Word>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val ex = o.optJSONObject("example")
            out.add(
                Word(
                    id = o.getInt("id"),
                    topic = o.optString("topic"),
                    hanzi = o.getString("hanzi"),
                    pinyin = o.optString("pinyin"),
                    readingVi = o.optString("readingVi"),
                    hanViet = o.optString("hanViet"),
                    meaningVi = o.optString("meaningVi"),
                    hsk = if (o.has("hsk") && !o.isNull("hsk")) o.getInt("hsk") else null,
                    unitNo = if (o.has("unitNo") && !o.isNull("unitNo")) o.getInt("unitNo") else null,
                    unit = o.optString("unit").ifEmpty { null },
                    exZh = ex?.optString("zh"),
                    exPy = ex?.optString("pinyin"),
                    exVi = ex?.optString("vi"),
                )
            )
        }
        return out
    }
}
