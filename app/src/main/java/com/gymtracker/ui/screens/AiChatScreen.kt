package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.theme.*
import com.gymtracker.ui.viewmodels.ProgressViewModel
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState
    
    // Calendar Setup
    val cal = Calendar.getInstance()
    val currentMonthName = SimpleDateFormat("MMMM yyyy", Locale.US).format(cal.time)
    
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0-indexed (Sunday = 0)
    
    val currentMonthFormat = SimpleDateFormat("yyyy-MM", Locale.US)
    val currentMonthPrefix = currentMonthFormat.format(Calendar.getInstance().time)
    
    val context = LocalContext.current

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Tokens.PaddingHorizontal, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STATISTICS",
                        style = Typography.displaySmall.copy(color = OffWhite)
                    )
                }
                Divider(color = Muted, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            NavBar(
                currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        if (stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Acid)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            
            // --- CALENDAR SECTION ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(Tokens.PaddingHorizontal)) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentMonthName,
                            style = Typography.titleMedium.copy(color = OffWhite, fontWeight = FontWeight.Bold)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = OffWhite)
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Streaks
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier.weight(1f).background(Dim, RoundedCornerShape(8.dp)).padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${stats.currentStreakWeeks} wk streak", style = Typography.bodyMedium.copy(color = OffWhite))
                            }
                        }
                        Box(
                            modifier = Modifier.weight(1f).background(Dim, RoundedCornerShape(8.dp)).padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🌙", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("${stats.restDays} rest days", style = Typography.bodyMedium.copy(color = OffWhite))
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Mini Calendar Grid
                    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { day ->
                            Text(text = day, style = Typography.bodySmall.copy(color = OffWhite.copy(alpha = 0.5f)), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Process Workout Dates for Calendar
                    val workoutDaysSet = stats.workoutDates
                        .filter { it.startsWith(currentMonthPrefix) }
                        .mapNotNull { it.takeLast(2).toIntOrNull() }
                        .toSet()
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var dayCounter = 1
                        val totalCells = daysInMonth + firstDayOfWeek
                        val rows = Math.ceil(totalCells / 7.0).toInt()
                        
                        for (week in 0 until rows) {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                for (dayOfWeek in 0..6) {
                                    val cellIndex = week * 7 + dayOfWeek
                                    if (cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                                        val isWorkout = workoutDaysSet.contains(dayCounter)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1f)
                                                .padding(2.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (isWorkout) Acid.copy(alpha = 0.8f) else Color.Transparent),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = dayCounter.toString(),
                                                style = Typography.bodySmall.copy(color = if (isWorkout) AppBlack else OffWhite.copy(alpha = 0.8f), fontWeight = if (isWorkout) FontWeight.Bold else FontWeight.Normal)
                                            )
                                        }
                                        dayCounter++
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            item { Divider(color = Muted, thickness = 8.dp, modifier = Modifier.padding(vertical = 16.dp)) }
            
            // --- BODY HEATMAP SECTION ---
            item {
                Column(modifier = Modifier.fillMaxWidth().padding(Tokens.PaddingHorizontal)) {
                    Text(
                        text = "Last 7 days body graph",
                        style = Typography.titleMedium.copy(color = OffWhite, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .background(Dim, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "Body Map",
                                modifier = Modifier.size(100.dp),
                                tint = Acid.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Top Muscle Hit:", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha=0.6f)))
                            Text(stats.topMuscleWeek, style = Typography.headlineSmall.copy(color = Acid, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            
            item { Divider(color = Muted, thickness = 8.dp, modifier = Modifier.padding(vertical = 16.dp)) }

            // --- ADVANCED STATISTICS SECTION ---
            item {
                val topMuscle = stats.topMusclesBySets.firstOrNull()
                val topExercise = stats.topExercisesBySets.firstOrNull()

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Advanced statistics",
                        style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.5f)),
                        modifier = Modifier.padding(horizontal = Tokens.PaddingHorizontal, vertical = 8.dp)
                    )
                    
                    StatMenuItem(
                        icon = Icons.Default.List,
                        title = "Set count per muscle group",
                        subtitle = if (topMuscle != null) "${topMuscle.first}: ${topMuscle.second} sets" else "No data yet",
                        onClick = { onNavigate("statDetail/muscles") }
                    )
                    Divider(color = Muted, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))
                    
                    StatMenuItem(
                        icon = Icons.Default.Face,
                        title = "Muscle distribution (Chart)",
                        subtitle = "Compare your current and previous muscle distributions.",
                        onClick = { onNavigate("statDetail/piechart") }
                    )
                    Divider(color = Muted, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))

                    StatMenuItem(
                        icon = Icons.Default.Person,
                        title = "Muscle distribution (Body)",
                        subtitle = "Weekly heat map of muscles worked.",
                        onClick = { onNavigate("statDetail/body") }
                    )
                    Divider(color = Muted, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))

                    StatMenuItem(
                        icon = Icons.Default.Build,
                        title = "Main exercises",
                        subtitle = if (topExercise != null) "${topExercise.first}: ${topExercise.second} sets" else "No data yet",
                        onClick = { onNavigate("statDetail/exercises") }
                    )
                    Divider(color = Muted, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))

                    StatMenuItem(
                        icon = Icons.Default.Star,
                        title = "Leaderboard Exercises",
                        subtitle = "View your personal records.",
                        onClick = { onNavigate("statDetail/leaderboard") }
                    )
                    Divider(color = Muted, thickness = 0.5.dp, modifier = Modifier.padding(start = 56.dp))

                    StatMenuItem(
                        icon = Icons.Default.Info,
                        title = "Monthly Report",
                        subtitle = String.format("Total Volume: %.0f KG", stats.monthlyVolumeKg),
                        onClick = { onNavigate("statDetail/monthly") }
                    )
                }
            }
        }
    }
}

@Composable
fun StatMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = Tokens.PaddingHorizontal, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = OffWhite.copy(alpha = 0.8f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = Typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Medium))
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = subtitle, style = Typography.bodySmall.copy(color = OffWhite.copy(alpha = 0.5f)))
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = OffWhite.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
