package com.nguyendp.hantu

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class Speaker(private val ctx: Context) {
    @Volatile private var ready = false
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(ctx) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val r = tts?.setLanguage(Locale.CHINA)
                ready = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
            }
        }
    }

    fun speak(text: String) {
        if (ready) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "word")
        } else {
            Toast.makeText(
                ctx,
                "Máy chưa có giọng đọc tiếng Trung. Hãy cài ở Cài đặt > Chuyển văn bản thành giọng nói.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
