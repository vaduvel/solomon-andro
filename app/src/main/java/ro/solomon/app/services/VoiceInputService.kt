package ro.solomon.app.services

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class VoiceInputService(private val context: Context) {

    enum class State {
        Idle, Listening, Processing, Result, Error, PermissionDenied, NotAvailable
    }

    data class Result(
        val text: String,
        val confidence: Float = 1.0f
    )

    data class VoiceState(
        val state: State = State.Idle,
        val partialText: String = "",
        val lastResult: Result? = null,
        val errorMessage: String? = null
    )

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    fun start(locale: Locale = Locale("ro", "RO")) {
        if (!hasPermission()) {
            _state.value = _state.value.copy(state = State.PermissionDenied, errorMessage = "Permisiune microfon lipsă")
            return
        }
        if (!isAvailable()) {
            _state.value = _state.value.copy(state = State.NotAvailable, errorMessage = "Speech recognition indisponibil")
            return
        }

        stop()

        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(buildListener())
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }

        _state.value = _state.value.copy(state = State.Listening, errorMessage = null)
        recognizer?.startListening(intent)
    }

    fun stop() {
        try { recognizer?.stopListening() } catch (_: Throwable) {}
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
    }

    fun cancel() {
        try { recognizer?.cancel() } catch (_: Throwable) {}
        try { recognizer?.destroy() } catch (_: Throwable) {}
        recognizer = null
        _state.value = _state.value.copy(state = State.Idle, partialText = "")
    }

    fun reset() {
        _state.value = VoiceState()
    }

    private fun buildListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(state = State.Processing)
        }

        override fun onError(error: Int) {
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Eroare audio"
                SpeechRecognizer.ERROR_CLIENT -> "Eroare client"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisiune microfon refuzată"
                SpeechRecognizer.ERROR_NETWORK -> "Eroare rețea"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout rețea"
                SpeechRecognizer.ERROR_NO_MATCH -> "N-am înțeles. Încearcă din nou."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recunoașterea e ocupată"
                SpeechRecognizer.ERROR_SERVER -> "Eroare server"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "N-am auzit nimic. Încearcă din nou."
                else -> "Eroare necunoscută ($error)"
            }
            _state.value = _state.value.copy(state = State.Error, errorMessage = msg)
        }

        override fun onResults(results: Bundle?) {
            val list = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val first = list?.firstOrNull { it.isNotBlank() }
            if (first != null) {
                val confidence = results.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)?.firstOrNull() ?: 1.0f
                _state.value = _state.value.copy(
                    state = State.Result,
                    lastResult = Result(first, confidence),
                    partialText = ""
                )
            } else {
                _state.value = _state.value.copy(
                    state = State.Error,
                    errorMessage = "N-am putut extrage text."
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val list = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val first = list?.firstOrNull() ?: return
            _state.value = _state.value.copy(partialText = first, state = State.Listening)
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }
}
