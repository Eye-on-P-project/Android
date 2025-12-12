package ac.sbmax002.eye_on.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import ac.sbmax002.eye_on.ui.settings.AlarmSound

/**
 * 알림음을 루프로 재생/중지하는 헬퍼
 */
class AlarmPlayer(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private var mediaPlayer: MediaPlayer? = null
    private var audioFocusRequest: AudioFocusRequest? = null
    private var currentSound: AlarmSound? = null
    private var currentVolume: Int = 0

    fun play(sound: AlarmSound, volumePercent: Int) {
        val normalizedVolume = volumePercent.coerceIn(0, 100)

        // 동일 설정으로 이미 재생 중이면 스킵
        if (isPlayingSafe() &&
            sound == currentSound &&
            normalizedVolume == currentVolume
        ) {
            return
        }

        stop()

        val resName = sound.fileName
            .removeSuffix(".mp3")
            .removeSuffix(".wav")
            .removeSuffix(".ogg")
        val resId = context.resources.getIdentifier(resName, "raw", context.packageName)

        if (resId == 0) {
            Log.e(TAG, "Sound resource not found for $resName")
            return
        }

        val player = MediaPlayer.create(context, resId)
        if (player == null) {
            Log.e(TAG, "Failed to create MediaPlayer for $resName")
            return
        }

        requestAudioFocus()

        val volume = (normalizedVolume / 100f).coerceIn(0f, 1f)
        player.setVolume(volume, volume)
        player.isLooping = true
        player.setOnErrorListener { _, what, extra ->
            Log.e(TAG, "MediaPlayer error what=$what extra=$extra")
            stopInternal()
            true
        }
        player.setOnCompletionListener {
            // 루프 재생이지만 에러 등으로 종료될 경우 방어적으로 정리
            stopInternal()
        }

        try {
            player.start()
            mediaPlayer = player
            currentSound = sound
            currentVolume = normalizedVolume
            Log.d(TAG, "Started alarm sound=${sound.name}, volume=$normalizedVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start MediaPlayer: ${e.message}", e)
            stopInternal()
        }
    }

    fun stop() {
        stopInternal()
    }

    fun isPlaying(): Boolean = isPlayingSafe()

    private fun isPlayingSafe(): Boolean {
        val player = mediaPlayer ?: return false
        return try {
            player.isPlaying
        } catch (e: IllegalStateException) {
            Log.w(TAG, "isPlaying() illegal state, cleaning up: ${e.message}")
            stopInternal()
            false
        } catch (e: Exception) {
            Log.w(TAG, "isPlaying() error, cleaning up: ${e.message}")
            stopInternal()
            false
        }
    }

    private fun stopInternal() {
        mediaPlayer?.run {
            try {
                stop()
            } catch (_: Exception) {
            }
            try {
                release()
            } catch (_: Exception) {
            }
        }
        mediaPlayer = null
        currentSound = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attributes)
                .build()
            audioFocusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            audioManager.abandonAudioFocus(null)
        }
        audioFocusRequest = null
    }

    companion object {
        private const val TAG = "AlarmPlayer"
    }
}

