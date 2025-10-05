package xyz.koleno.internetradioplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import dagger.hilt.android.AndroidEntryPoint
import xyz.koleno.internetradioplayer.data.Station
import xyz.koleno.internetradioplayer.utils.Preferences
import xyz.koleno.internetradioplayer.utils.restoreDefaultCertificateValidation
import xyz.koleno.internetradioplayer.utils.validateAllCertificates
import javax.inject.Inject

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@AndroidEntryPoint
class StreamingService : Service() {

    private val binder = StreamingServiceBinder()
    private lateinit var exoPlayer: ExoPlayer
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var notificationBuilder: Notification.Builder
    private lateinit var notificationManager: NotificationManager
    private var callback: ((text: String) -> Unit)? = null
    private var errorCallback: ((error: String) -> Unit)? = null
    private var playCallback: (() -> Unit)? = null
    private var stopCallback: (() -> Unit)? = null
    private var errorRetry = 0
    var currentlyPlaying: Station? = null
        private set

    @Inject
    lateinit var preferences: Preferences

    private val mediaInfoRunnable = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                val contentDescription = java.lang.StringBuilder()
                val metadata = exoPlayer.mediaMetadata
                if (metadata.artist.isNullOrBlank().not()) {
                    contentDescription.append(" " + metadata.artist)
                }
                if (metadata.title.isNullOrBlank().not()) {
                    contentDescription.append(" " + metadata.title)
                }
                notificationBuilder.setContentTitle(
                    getString(
                        R.string.playing,
                        currentlyPlaying?.name.orEmpty()
                    )
                )
                notificationBuilder.setContentText(contentDescription.trim())
                callback?.invoke(contentDescription.trim().toString())
            } else {
                notificationBuilder.setContentTitle(getString(R.string.not_playing))
                notificationBuilder.setContentText(null)
            }
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
            handler.postDelayed(this, MEDIA_INFO_INTERVAL)
        }

    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // set up notification for running service
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_LOW
        val mChannel = NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), importance)
        notificationManager.createNotificationChannel(mChannel)

        notificationBuilder = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.not_playing))
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )

            // close button
            .addAction(
                Notification.Action.Builder(
                    null, getString(R.string.close), PendingIntent.getService(
                        this, 0, Intent(
                            this,
                            StreamingService::class.java
                        ).apply {
                            putExtra(EXTRA_CLOSE_SERVICE, true)
                        }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                ).build()
            )
            .setSmallIcon(R.drawable.ic_launcher_foreground)

        startForeground(NOTIFICATION_ID, notificationBuilder.build())

        // HTTPS certificate handling
        if (preferences.isIgnoreSecurityEnabled()) {
            validateAllCertificates()
        } else {
            restoreDefaultCertificateValidation()
        }

        // setup exoplayer
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true // let exoPlayer handle audio focus
        )

        exoPlayer.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                if (errorRetry < RETRY_MAX) {
                    exoPlayer.prepare()
                    exoPlayer.play()
                    errorRetry++
                } else {
                    errorRetry = 0
                    errorCallback?.invoke(error.message.orEmpty() + " " + error.cause?.message)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    playCallback?.invoke()
                }
            }
        })

        // setup updates
        handler.postDelayed(mediaInfoRunnable, MEDIA_INFO_INTERVAL)
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (exoPlayer.isPlaying) exoPlayer.stop()
        exoPlayer.release()

        super.onDestroy()
    }

    private fun buildMediaSource(uri: String): MediaSource {
        val dataSourceFactory: DefaultHttpDataSource.Factory = DefaultHttpDataSource.Factory()
        dataSourceFactory.setUserAgent(USER_AGENT)
        return HlsMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
    }

    fun play(station: Station) {
        currentlyPlaying = station

        if (exoPlayer.isPlaying) {
            exoPlayer.stop()
        }

        if (station.uri.isEmpty()) {
            return // nothing to play
        }

        if (station.uri.contains("m3u8")) {
            exoPlayer.setMediaSource(buildMediaSource(station.uri))
            exoPlayer.prepare()
        } else {
            exoPlayer.setMediaItem(MediaItem.fromUri(station.uri))
            exoPlayer.prepare()
        }

        exoPlayer.play()
    }

    fun stop() {
        exoPlayer.stop()
    }

    fun showNotification() {
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    fun setListener(listener: ((text: String) -> Unit)?) {
        callback = listener
    }

    fun setErrorListener(listener: ((error: String?) -> Unit)) {
        errorCallback = listener
    }

    fun setPlayListener(listener: () -> Unit) {
        playCallback = listener
    }

    fun setStopListener(listener: () -> Unit) {
        stopCallback = listener
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            if (it.hasExtra(EXTRA_CLOSE_SERVICE)) {
                stopCallback?.invoke() // also notify activity if it is bound to the service
                stopSelf()
            }
        }

        return super.onStartCommand(intent, flags, startId)
    }

    inner class StreamingServiceBinder : Binder() {
        fun getService(): StreamingService = this@StreamingService
    }

    companion object {
        private const val EXTRA_CLOSE_SERVICE = "extraCloseService"
        private const val RETRY_MAX = 3
        private const val MEDIA_INFO_INTERVAL = 5000L
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "channelId"
        private const val USER_AGENT =
            "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/109.0"
    }
}