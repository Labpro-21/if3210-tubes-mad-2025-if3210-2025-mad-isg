package com.example.purrytify.service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.purrytify.models.Song
import com.example.purrytify.receivers.MediaButtonReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException

class MediaPlayerService : Service() {
    private val TAG = "MediaPlayerService"
    private val mediaPlayer = MediaPlayer()
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationManager: PurrytifyNotificationManager
    private lateinit var mediaButtonReceiver: MediaButtonReceiver
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var progressUpdateJob: Job? = null

    // State flows
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration

    private val _reachedEndOfPlayback = MutableStateFlow(false)
    val reachedEndOfPlayback: StateFlow<Boolean> = _reachedEndOfPlayback

    private val _currentPlayingId = MutableStateFlow<Long?>(null)

    // Bonus features state
    private var shuffleEnabled = false
    private var repeatMode = 0 // 0: off, 1: repeat all, 2: repeat one

    private val binder = MediaPlayerBinder()
    private lateinit var localBroadcastManager: LocalBroadcastManager

    inner class MediaPlayerBinder : Binder() {
        fun getService(): MediaPlayerService = this@MediaPlayerService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        // Initialize MediaSession
        mediaSession = MediaSessionCompat(this, "PurrytifyMediaSession")
        mediaSession.setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        // Set up MediaSession callback
        mediaSession.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                togglePlayPause()
            }

            override fun onPause() {
                togglePlayPause()
            }

            override fun onSkipToNext() {
                handleNextAction()
            }

            override fun onSkipToPrevious() {
                handlePreviousAction()
            }

            override fun onStop() {
                handleStopAction()
            }

            override fun onSeekTo(pos: Long) {
                seekTo(pos.toInt())
            }
        })

        mediaSession.isActive = true

        // Initialize notification manager
        notificationManager = PurrytifyNotificationManager(this, mediaSession)

        // Initialize media button receiver
        mediaButtonReceiver = MediaButtonReceiver()
        registerMediaButtonReceiver()

        localBroadcastManager = LocalBroadcastManager.getInstance(this)

        setupMediaPlayerListeners()
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private fun registerMediaButtonReceiver() {
        val filter = IntentFilter().apply {
            addAction(PurrytifyNotificationManager.ACTION_PLAY_PAUSE)
            addAction(PurrytifyNotificationManager.ACTION_NEXT)
            addAction(PurrytifyNotificationManager.ACTION_PREVIOUS)
            addAction(PurrytifyNotificationManager.ACTION_STOP)
        }

        // Register receiver with proper flags for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ (API 33+) requires explicit exported flag
            ContextCompat.registerReceiver(
                this,
                mediaButtonReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        } else {
            // Older Android versions
            registerReceiver(mediaButtonReceiver, filter)
        }
    }

    private fun setupMediaPlayerListeners() {
        // Set up completion listener
        mediaPlayer.setOnCompletionListener {
            Log.d(TAG, "Song completed playback")
            _isPlaying.value = false
            hideNotification()

            // Handle repeat one mode
            if (repeatMode == 2) {
                Log.d(TAG, "Repeat One mode active, replaying current song")
                _currentSong.value?.let {
                    playAgain()
                }
            } else {
                // Send broadcast to notify of song completion
                val intent = Intent("com.example.purrytify.SONG_COMPLETED")

                if (repeatMode == 0) {
                    _reachedEndOfPlayback.value = true
                    intent.putExtra("END_OF_PLAYBACK", true)
                }

                intent.putExtra("COMPLETED_SONG_ID", _currentPlayingId.value)
                localBroadcastManager.sendBroadcast(intent)
            }
        }

        // Set up error listener
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
            hideNotification()
            true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service started with action: ${intent?.getStringExtra("action")}")

        intent?.getStringExtra("action")?.let { action ->
            when (action) {
                "TOGGLE_PLAY_PAUSE" -> togglePlayPause()
                "NEXT" -> handleNextAction()
                "PREVIOUS" -> handlePreviousAction()
                "STOP" -> handleStopAction()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        Log.d(TAG, "Service bound")
        return binder
    }

    fun playOnlineSong(
        audioUrl: String,
        title: String,
        artist: String,
        coverUrl: String = ""
    ) {
        try {
            Log.d(TAG, "Playing online song: $title - $artist, URL: $audioUrl")

            mediaPlayer.reset()
            mediaPlayer.setDataSource(audioUrl)

            mediaPlayer.prepareAsync()
            mediaPlayer.setOnPreparedListener {
                it.start()

                val song = Song(
                    id = -1,
                    title = title,
                    artist = artist,
                    coverUrl = coverUrl,
                    filePath = audioUrl,
                    duration = it.duration.toLong(),
                    isPlaying = true,
                    isLiked = false,
                    isOnline = true,
                    lastPlayed = System.currentTimeMillis()
                )

                _currentSong.value = song
                _isPlaying.value = true
                _duration.value = it.duration

                startPositionTracking()
                showNotification()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing online song", e)
        }
    }

    fun playSong(song: Song) {
        try {
            Log.d(TAG, "Playing song: ${song.title}, path: ${song.filePath}")
            _reachedEndOfPlayback.value = false

            mediaPlayer.reset()
            _currentPlayingId.value = song.id

            if (song.filePath.startsWith("content://")) {
                val uri = Uri.parse(song.filePath)
                val contentResolver = applicationContext.contentResolver
                val afd = contentResolver.openFileDescriptor(uri, "r")

                if (afd != null) {
                    mediaPlayer.setDataSource(afd.fileDescriptor)
                    afd.close()
                } else {
                    throw IOException("Cannot open file descriptor for URI: ${song.filePath}")
                }
            } else {
                mediaPlayer.setDataSource(song.filePath)
            }

            mediaPlayer.prepare()
            mediaPlayer.start()

            _currentSong.value = song
            _isPlaying.value = true
            _duration.value = mediaPlayer.duration
            Log.d(TAG, "Song duration: ${mediaPlayer.duration}ms")

            startPositionTracking()
            showNotification()

        } catch (e: IOException) {
            Log.e(TAG, "Error playing song: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun playAgain() {
        try {
            mediaPlayer.seekTo(0)
            mediaPlayer.start()
            _isPlaying.value = true
            startPositionTracking()
            showNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error replaying song: ${e.message}")
        }
    }

    fun togglePlayPause() {
        if (mediaPlayer.isPlaying) {
            Log.d(TAG, "Pausing playback")
            mediaPlayer.pause()
            _isPlaying.value = false
        } else {
            Log.d(TAG, "Resuming playback")
            mediaPlayer.start()
            _isPlaying.value = true
            startPositionTracking()
        }
        showNotification()
    }

    fun seekTo(position: Int) {
        Log.d(TAG, "Seeking to position: ${position}ms")
        mediaPlayer.seekTo(position)
        _currentPosition.value = position
        showNotification()
    }

    private fun handleNextAction() {
        // Send broadcast to MainViewModel to handle next song logic
        val intent = Intent("com.example.purrytify.MEDIA_BUTTON_ACTION")
        intent.putExtra("action", "NEXT")
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun handlePreviousAction() {
        // Send broadcast to MainViewModel to handle previous song logic
        val intent = Intent("com.example.purrytify.MEDIA_BUTTON_ACTION")
        intent.putExtra("action", "PREVIOUS")
        localBroadcastManager.sendBroadcast(intent)
    }

    private fun handleStopAction() {
        Log.d(TAG, "Stop action received")
        stopPlayback()
        hideNotification()

        // Send broadcast to update UI
        val intent = Intent("com.example.purrytify.MEDIA_BUTTON_ACTION")
        intent.putExtra("action", "STOP")
        localBroadcastManager.sendBroadcast(intent)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        Log.d(TAG, "Shuffle mode set to: $enabled")
        shuffleEnabled = enabled
    }

    fun setRepeatMode(mode: Int) {
        Log.d(TAG, "Repeat mode set to: $mode")
        repeatMode = mode
    }

    fun resetEndOfPlaybackFlag() {
        _reachedEndOfPlayback.value = false
    }

    fun stopPlayback() {
        try {
            if (mediaPlayer.isPlaying) {
                val totalDuration = mediaPlayer.duration
                mediaPlayer.seekTo(totalDuration)
                mediaPlayer.pause()
                _isPlaying.value = false
                _currentPosition.value = totalDuration
                _reachedEndOfPlayback.value = true
                Log.d(TAG, "Playback stopped and moved to end of track")
            } else {
                Log.d(TAG, "No need to stop playback, already paused")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping playback: ${e.message}")
        }
    }

    private fun startPositionTracking() {
        progressUpdateJob?.cancel()
        progressUpdateJob = serviceScope.launch {
            while (_isPlaying.value && !mediaPlayer.isLooping) {
                try {
                    if (mediaPlayer.isPlaying) {
                        _currentPosition.value = mediaPlayer.currentPosition

                        // Update notification with progress every 5 seconds to avoid too frequent updates
                        if (_currentPosition.value % 5000 < 500) {
                            showNotification()
                        }
                    }
                    delay(500)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in position tracking: ${e.message}")
                    break
                }
            }
        }
    }

    private fun showNotification() {
        _currentSong.value?.let { song ->
            notificationManager.showNotification(
                song,
                _isPlaying.value,
                _currentPosition.value.toLong(),
                _duration.value.toLong()
            )
        }
    }

    private fun hideNotification() {
        notificationManager.hideNotification()
    }

    override fun onDestroy() {
        Log.d(TAG, "Service destroyed")

        // Clean up
        progressUpdateJob?.cancel()
        mediaPlayer.release()
        mediaSession.release()
        hideNotification()

        try {
            unregisterReceiver(mediaButtonReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering media button receiver: ${e.message}")
        }

        super.onDestroy()
    }
}