package com.gymtracker.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.media.NowPlayingManager
import com.gymtracker.ui.theme.OffWhite
import kotlinx.coroutines.delay

@Composable
fun NowPlayingBar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val currentTrack by NowPlayingManager.currentTrack.collectAsState()
    
    var isGranted by remember { mutableStateOf(NowPlayingManager.isPermissionGranted(context)) }
    
    // For progress bar tracking
    var positionMs by remember { mutableStateOf(0L) }
    var durationMs by remember { mutableStateOf(0L) }

    var showControls by remember { mutableStateOf(false) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    LaunchedEffect(Unit) {
        while(!isGranted) {
            delay(2000)
            isGranted = NowPlayingManager.isPermissionGranted(context)
            if (isGranted) {
                NowPlayingManager.startListening(context)
            }
        }
    }

    // Timer to update progress bar smoothly
    LaunchedEffect(currentTrack?.isPlaying) {
        while (currentTrack?.isPlaying == true) {
            positionMs = NowPlayingManager.getPosition()
            durationMs = NowPlayingManager.getDuration()
            delay(1000)
        }
        positionMs = NowPlayingManager.getPosition()
        durationMs = NowPlayingManager.getDuration()
    }
    
    LaunchedEffect(currentTrack?.title) {
        positionMs = NowPlayingManager.getPosition()
        durationMs = NowPlayingManager.getDuration()
    }

    AnimatedVisibility(
        visible = !isGranted || currentTrack != null,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 2 },
        exit = fadeOut(tween(300)) + slideOutVertically(tween(300)) { it / 2 }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF2B2E42))
                .clickable { showControls = true }
        ) {
            if (!isGranted) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Now Playing Sync", color = OffWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Enable Notification Access", color = OffWhite.copy(alpha = 0.6f), fontSize = 12.sp)
                    }
                    TextButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF1DB954))
                    ) {
                        Text("Enable", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                currentTrack?.let { track ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.animation.Crossfade(targetState = showControls) { controlsVisible ->
                                if (controlsVisible) {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        IconButton(onClick = { NowPlayingManager.skipToPrevious(); showControls = true }) {
                                            Icon(Icons.Default.KeyboardArrowLeft, "Previous", tint = OffWhite, modifier = Modifier.size(32.dp))
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Box(
                                            modifier = Modifier.size(40.dp).clip(CircleShape).clickable { NowPlayingManager.playPause(); showControls = true },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (track.isPlaying) {
                                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Box(modifier = Modifier.width(6.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(OffWhite))
                                                    Box(modifier = Modifier.width(6.dp).height(20.dp).clip(RoundedCornerShape(2.dp)).background(OffWhite))
                                                }
                                            } else {
                                                Icon(Icons.Default.PlayArrow, "Play", tint = OffWhite, modifier = Modifier.size(32.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        IconButton(onClick = { NowPlayingManager.skipToNext(); showControls = true }) {
                                            Icon(Icons.Default.KeyboardArrowRight, "Next", tint = OffWhite, modifier = Modifier.size(32.dp))
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = track.title,
                                            color = OffWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("•", color = OffWhite.copy(alpha = 0.5f), fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = track.artist,
                                            color = OffWhite.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                            }
                        }
                        
                        val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = Color(0xFF1DB954),
                            trackColor = Color.White.copy(alpha = 0.1f)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
