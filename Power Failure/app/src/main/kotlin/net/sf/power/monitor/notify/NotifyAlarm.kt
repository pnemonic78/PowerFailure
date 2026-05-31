package net.sf.power.monitor.notify

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat.createAttributionContext
import timber.log.Timber
import java.io.IOException

class NotifyAlarm(private val context: Context) {

    private var ringtoneUri: Uri? = null
    private var ringtone: MediaPlayer? = null

    fun play(ringtoneUri: Uri) {
        val context = createAttributionContext(context, "media")
        playTone(context, ringtoneUri)
    }

    fun stop() {
        stopTone()
    }

    private fun playTone(context: Context, ringtoneUri: Uri) {
        val ringtone = getRingtone(context, ringtoneUri)
        try {
            val retriever = MediaMetadataRetriever().apply {
                setDataSource(context, ringtoneUri)
            }
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            Timber.v("play tone: %s", title ?: "(none)")
        } catch (e: Exception) {
            Timber.v(e, "play tone: %s", ringtone)
        }
        if (ringtone != null && !ringtone.isPlaying) {
            ringtone.start()
        }
    }

    private fun getRingtone(context: Context, uri: Uri): MediaPlayer? {
        if (uri != this.ringtoneUri) {
            this.ringtone = null
        }
        var ringtone = this.ringtone
        if (ringtone == null) {
            try {
                ringtone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    MediaPlayer(context)
                } else {
                    MediaPlayer()
                }.apply {
                    val audioStreamType = AudioManager.STREAM_ALARM
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setLegacyStreamType(audioStreamType)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()

                    setDataSource(context, uri)
                    setAudioAttributes(audioAttributes)
                    isLooping = true
                    prepare()
                }
            } catch (e: IOException) {
                Timber.e(e, "error preparing ringtone: %s for %s", e.message, uri)
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