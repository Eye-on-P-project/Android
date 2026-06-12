package ac.sbmax002.eye_on.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class VoiceCompanion(context: Context) {

    private var initialized = false
    private var pendingText: String? = null
    private var tts: TextToSpeech? = null
    @Volatile
    private var speaking = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                initialized = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        speaking = true
                        Log.d(TAG, "TTS started. utteranceId=$utteranceId")
                    }

                    override fun onDone(utteranceId: String?) {
                        speaking = false
                        Log.d(TAG, "TTS completed. utteranceId=$utteranceId")
                    }

                    override fun onError(utteranceId: String?) {
                        speaking = false
                        Log.e(TAG, "TTS error. utteranceId=$utteranceId")
                    }
                })
                val languageResult = tts?.setLanguage(Locale.KOREAN)
                if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
                    languageResult == TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    Log.w(TAG, "Korean TTS language is not fully supported on this device.")
                }
                pendingText?.let { text ->
                    pendingText = null
                    speak(text)
                }
            } else {
                Log.e(TAG, "Failed to initialize TextToSpeech. status=$status")
            }
        }
    }

    fun speak(text: String) {
        val normalizedText = text.trim()
        if (normalizedText.isBlank()) return

        val currentTts = tts
        if (!initialized || currentTts == null) {
            pendingText = normalizedText
            return
        }

        Log.d(TAG, "TTS speak request. length=${normalizedText.length}, text=$normalizedText")
        currentTts.speak(
            normalizedText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "eye_on_voice_${System.currentTimeMillis()}"
        )
    }

    fun stop() {
        pendingText = null
        speaking = false
        tts?.stop()
    }

    fun isSpeaking(): Boolean {
        return speaking || tts?.isSpeaking == true
    }

    fun shutdown() {
        pendingText = null
        speaking = false
        tts?.stop()
        tts?.shutdown()
        tts = null
        initialized = false
    }

    companion object {
        private const val TAG = "VoiceCompanion"
    }
}
