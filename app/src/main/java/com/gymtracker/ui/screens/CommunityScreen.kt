package com.gymtracker.ui.screens

import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.viewmodels.CommunityViewModel
import com.gymtracker.auth.SessionManager

@Composable
fun CommunityScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: CommunityViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

    Scaffold(
        containerColor = AppBlack,
        bottomBar = { NavBar(currentRoute = currentRoute, onNavigate = onNavigate) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Tokens.PaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Text(
                text = "LEADERBOARD",
                style = Typography.displayMedium.copy(color = OffWhite, fontWeight = FontWeight.Black)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            LeaderboardView(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardView(viewModel: CommunityViewModel) {
    val mockUsers by viewModel.leaderboard.collectAsState(initial = emptyList())
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    var userToStalk by remember { mutableStateOf<com.gymtracker.network.LeaderboardUser?>(null) }
    var showStalkDialog by remember { mutableStateOf(false) }
    var showWorkoutSheet by remember { mutableStateOf(false) }
    
    if (showStalkDialog && userToStalk != null) {
        AlertDialog(
            onDismissRequest = { showStalkDialog = false },
            containerColor = AppBlack,
            titleContentColor = OffWhite,
            textContentColor = OffWhite,
            title = { Text("Want to stalk them?", fontFamily = AnybodyFamily, fontWeight = FontWeight.Bold) },
            text = { Text("See what ${userToStalk?.name} lifted in their most recent session.") },
            confirmButton = {
                TextButton(onClick = { 
                    showStalkDialog = false
                    if (!userToStalk?.latestWorkoutJson.isNullOrEmpty()) {
                        showWorkoutSheet = true
                    }
                }) {
                    Text("Sure", color = Acid, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStalkDialog = false }) {
                    Text("Nah", color = Color.Gray)
                }
            }
        )
    }
    
    if (showWorkoutSheet && userToStalk != null) {
        ModalBottomSheet(
            onDismissRequest = { showWorkoutSheet = false },
            containerColor = AppBlack,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.DarkGray) }
        ) {
            StalkBottomSheetContent(userToStalk!!)
        }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp),
            modifier = Modifier.fillMaxSize()
        ) {
        itemsIndexed(mockUsers) { index, user ->
            val rank = index + 1
            val (cardColor, badgeColor) = when(rank) {
                1 -> Pair(Color(0xFF2C2510), Color(0xFFFFD700)) // Gold
                2 -> Pair(Color(0xFF1F2124), Color(0xFFC0C0C0)) // Silver
                3 -> Pair(Color(0xFF261D15), Color(0xFFCD7F32)) // Bronze
                else -> Pair(Dim, OffWhite.copy(alpha = 0.3f))
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardColor)
                    .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .clickable { 
                        userToStalk = user
                        showStalkDialog = true
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rank Badge
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.2f))
                        .border(1.dp, badgeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#$rank",
                        style = Typography.titleMedium.copy(color = badgeColor, fontWeight = FontWeight.Bold)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Name & Stats
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = user.name,
                        style = Typography.titleLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Total Volume",
                        style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.6f))
                    )
                }
                
                // Score
                Text(
                    text = "${user.xp} KG",
                    style = Typography.titleMedium.copy(color = Acid, fontWeight = FontWeight.Black)
                )
            }
        }
    }
}
}

@Composable
fun StalkBottomSheetContent(user: com.gymtracker.network.LeaderboardUser) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "${user.name}'s Workout",
            style = Typography.titleLarge.copy(color = Acid, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            try {
                val jsonStr = user.latestWorkoutJson ?: "[]"
                val array = org.json.JSONArray(jsonStr)
                if (array.length() == 0) {
                    item {
                        Text(text = "No recent workout data available.", color = Color.Gray)
                    }
                } else {
                    for (i in 0 until array.length()) {
                        val exercise = array.getJSONObject(i)
                        val title = exercise.optString("title", "Unknown Exercise")
                        val sets = exercise.optJSONArray("sets") ?: org.json.JSONArray()
                        
                        item {
                            Text(
                                text = title,
                                style = Typography.titleMedium.copy(color = OffWhite, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                        }
                        
                        for (j in 0 until sets.length()) {
                            val setObj = sets.getJSONObject(j)
                            val weight = setObj.optDouble("weight_kg", 0.0)
                            val reps = setObj.optInt("reps", 0)
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp, horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "Set ${j + 1}", color = Color.Gray, fontSize = 14.sp)
                                    Text(text = "${weight}kg x $reps", color = OffWhite, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                item {
                    Text(text = "Failed to load workout details.", color = Color.Red)
                }
            }
            item {
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
