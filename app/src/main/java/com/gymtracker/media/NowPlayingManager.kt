package com.gymtracker.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.provider.Settings
import com.gymtracker.network.CommunityRepository
import com.gymtracker.services.MediaNotificationListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class NowPlayingTrack(
    val title: String,
    val artist: String,
    val album: String?,
    val isPlaying: Boolean,
    val appPackageName: String
) {
    fun toJsonString(): String {
        val obj = JSONObject()
        obj.put("title", title)
        obj.put("artist", artist)
        obj.put("album", album)
        obj.put("isPlaying", isPlaying)
        obj.put("appPackageName", appPackageName)
        return obj.toString()
    }

    companion object {
        fun fromJsonString(json: String?): NowPlayingTrack? {
            if (json.isNullOrEmpty()) return null
            return try {
                val obj = JSONObject(json)
                NowPlayingTrack(
                    title = obj.optString("title", "Unknown"),
                    artist = obj.optString("artist", "Unknown"),
                    album = obj.optString("album", null),
                    isPlaying = obj.optBoolean("isPlaying", false),
                    appPackageName = obj.optString("appPackageName", "")
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

object NowPlayingManager {

    private val _currentTrack = MutableStateFlow<NowPlayingTrack?>(null)
    val currentTrack: StateFlow<NowPlayingTrack?> = _currentTrack.asStateFlow()

    private var activeController: MediaController? = null
    private var mediaSessionManager: MediaSessionManager? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val repo = CommunityRepository()

    fun isPermissionGranted(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        return enabledListeners?.contains(context.packageName) == true
    }

    fun startListening(context: Context) {
        if (!isPermissionGranted(context)) return

        try {
            mediaSessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, MediaNotificationListener::class.java)

            mediaSessionManager?.addOnActiveSessionsChangedListener({ controllers ->
                updateControllers(controllers)
            }, componentName)

            val controllers = mediaSessionManager?.getActiveSessions(componentName)
            if (controllers != null) {
                updateControllers(controllers)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun updateControllers(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = null

        if (controllers.isNullOrEmpty()) {
            updateTrack(null)
            return
        }

        // Try to find Spotify first, or fallback to the first active one
        val preferredController = controllers.firstOrNull { it.packageName.contains("spotify", ignoreCase = true) }
            ?: controllers.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
            ?: controllers.firstOrNull()

        activeController = preferredController
        activeController?.registerCallback(controllerCallback)
        
        // Initial update
        handleMetadataChange(activeController?.metadata)
        handlePlaybackStateChange(activeController?.playbackState)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            handleMetadataChange(metadata)
        }

        override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
            handlePlaybackStateChange(state)
        }
    }

    private fun handleMetadataChange(metadata: MediaMetadata?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        val album = metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM)

        if (title != null || artist != null) {
            val isPlaying = activeController?.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
            val newTrack = NowPlayingTrack(
                title = title ?: "Unknown Song",
                artist = artist ?: "Unknown Artist",
                album = album,
                isPlaying = isPlaying,
                appPackageName = activeController?.packageName ?: ""
            )
            updateTrack(newTrack)
        } else {
            updateTrack(null)
        }
    }

    private fun handlePlaybackStateChange(state: android.media.session.PlaybackState?) {
        val current = _currentTrack.value
        if (current != null) {
            val isPlaying = state?.state == android.media.session.PlaybackState.STATE_PLAYING
            if (current.isPlaying != isPlaying) {
                updateTrack(current.copy(isPlaying = isPlaying))
            }
        }
    }

    private fun updateTrack(track: NowPlayingTrack?) {
        if (_currentTrack.value == track) return
        _currentTrack.value = track
        
        // Sync to backend whenever track changes or play state changes
        scope.launch {
            repo.syncNowPlaying(track?.toJsonString())
        }
    }

    fun playPause() {
        val state = activeController?.playbackState?.state
        if (state == android.media.session.PlaybackState.STATE_PLAYING) {
            activeController?.transportControls?.pause()
        } else {
            activeController?.transportControls?.play()
        }
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun getDuration(): Long {
        return activeController?.metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
    }

    fun getPosition(): Long {
        return activeController?.playbackState?.position ?: 0L
    }
}
