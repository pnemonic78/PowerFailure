package net.sf.power.monitor.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import com.github.media.RingtoneCompat
import com.github.media.RingtoneManager
import timber.log.Timber
import java.io.IOException

class NotifyAlarm(private val context: Context) {

    private var ringtone: RingtoneCompat? = null

    val isPlaying: Boolean get() = ringtone?.isPlaying == true

    fun play(ringtoneUri: Uri) {
        playTone(context, ringtoneUri)
    }

    fun stop() {
        stopTone()
    }

    private fun playTone(context: Context, ringtoneUri: Uri) {
        val ringtone = getRingtone(context, ringtoneUri) ?: return
        try {
            val title = ringtone.getTitle()
            Timber.v("play tone: %s", title ?: "(none)")
        } catch (e: Exception) {
            Timber.v(e, "play tone: %s", ringtone)
        }
        if (!ringtone.isPlaying) {
            ringtone.play()
        }
    }

    private fun getRingtone(context: Context, uri: Uri): RingtoneCompat? {
        var ringtone = this.ringtone
        if (ringtone == null) {
            try {
                ringtone = RingtoneManager.getRingtone(context, "media", uri).apply {
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(AudioManager.STREAM_ALARM)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()

                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                }
            } catch (e: IOException) {
                Timber.e(e, "error preparing ringtone: %s", uri)
            }
            this.ringtone = ringtone
        }
        return ringtone
    }

    private fun stopTone() {
        Timber.v("stop tone")
        val ringtone = this.ringtone
        if (ringtone != null && ringtone.isPlaying) {
            ringtone.stop()
        }
        this.ringtone = null
    }
}