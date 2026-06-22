package ro.solomon.app.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

object SolomonTTS : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var ready = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking

    private val _isEnabled = MutableStateFlow(true)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    fun init(context: Context) {
        if (tts != null) return
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("ro", "RO"))
            ready = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
                ready = true
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) { _isSpeaking.value = true }
                override fun onDone(utteranceId: String?) { _isSpeaking.value = false }
                override fun onError(utteranceId: String?) { _isSpeaking.value = false }
            })
        }
    }

    fun speak(text: String) {
        if (!ready || !_isEnabled.value) return
        val cleaned = cleanText(text)
        if (cleaned.isBlank()) return
        _isSpeaking.value = true
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, "solomon_${System.currentTimeMillis()}")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun toggle() {
        _isEnabled.value = !_isEnabled.value
        if (!_isEnabled.value) stop()
    }

    private fun cleanText(text: String): String {
        return text
            .replace("**", "")
            .replace("__", "")
            .replace("<TOOL>", "")
            .replace("</TOOL>", "")
            .replace("\n", " ")
            .replace("RON", "lei")
            .replace("\u2192", "c\u0103tre")
            .replace("\u26A0\uFE0F", "")
            .replace("\u2705", "")
            .replace("\uD83D\uDEA8", "")
            .trim()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
