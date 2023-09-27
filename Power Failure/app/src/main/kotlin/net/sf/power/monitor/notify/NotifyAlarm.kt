package net.sf.power.monitor.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import timber.log.Timber

class NotifyAlarm(private val context: Context) {

    private var ringtoneUri: Uri? = null
    private var ringtone: Ringtone? = null

    fun play(ringtoneUri: Uri) {
        playTone(context, ringtoneUri)
    }

    fun stop() {
        stopTone()
    }

    private fun playTone(context: Context, ringtoneUri: Uri) {
        val ringtone = getRingtone(context, ringtoneUri)
        try {
            Timber.v("play tone: %s", ringtone?.getTitle(context) ?: "(none)")
        } catch (e: Exception) {
            Timber.v("play tone: %s", ringtone)
        }
        if (ringtone != null && !ringtone.isPlaying) {
            ringtone.play()
        }
    }

    private fun getRingtone(context: Context, ringtoneUri: Uri): Ringtone? {
        if (ringtoneUri != this.ringtoneUri) {
            this.ringtone = null
        }
        var ringtone = this.ringtone
        if (ringtone == null) {
            ringtone = RingtoneManager.getRingtone(context, ringtoneUri)
            if (ringtone != null) {
                val audioStreamType = AudioManager.STREAM_ALARM
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setLegacyStreamType(audioStreamType)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
                ringtone.audioAttributes = audioAttributes
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