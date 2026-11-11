package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechOutput(private val context: Context) {

    private val tag = "SpeechOutput"
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    var isVoiceOutputEnabled = true
    var speechRate: Float = 1.0f
    var speechPitch: Float = 1.0f

    init {
        initTts()
    }

    private fun initTts() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.let { engine ->
                    val result = engine.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w(tag, "Default language not supported by TTS, falling back to US English.")
                        engine.setLanguage(Locale.US)
                    }
                    engine.setSpeechRate(speechRate)
                    engine.setPitch(speechPitch)
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            _isSpeaking.value = true
                        }

                        override fun onDone(utteranceId: String?) {
                            _isSpeaking.value = false
                        }

                        override fun onError(utteranceId: String?) {
                            _isSpeaking.value = false
                        }
                    })
                    isInitialized = true
                }
            } else {
                Log.e(tag, "TextToSpeech initialization failed with status: $status")
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!isVoiceOutputEnabled) return
        if (!isInitialized || tts == null) {
            initTts()
        }

        // Clean text of emojis / code fences for pleasant TTS
        val cleaned = text.replace(Regex("```[\\s\\S]*?```"), "")
            .replace(Regex("[*#_`•]"), " ")
            .trim()

        if (cleaned.isBlank()) return

        val utteranceId = "wibes_${System.currentTimeMillis()}"
        tts?.speak(cleaned, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        _isSpeaking.value = false
    }

    fun updateSettings(rate: Float, pitch: Float) {
        speechRate = rate
        speechPitch = pitch
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
