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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
        while(true) {
            viewModel.refresh()
            kotlinx.coroutines.delay(10000L)
        }
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
            Spacer(modifier = Modifier.height(16.dp))
            
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
    var selectedTab by remember { mutableStateOf(0) }
    
    val sortedUsers = remember(mockUsers, selectedTab) {
        when (selectedTab) {
            0 -> mockUsers.sortedByDescending { it.xp }
            1 -> mockUsers.sortedByDescending { it.benchPr + it.deadliftPr + it.squatPr }
            else -> mockUsers.sortedByDescending { 
                if (it.bodyWeight > 0f) it.xp / it.bodyWeight else 0f 
            }
        }
    }
    
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = Color.Transparent,
        contentColor = Acid,
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                color = Acid,
                height = 3.dp
            )
        },
        divider = { Divider(color = Muted, thickness = 1.dp) },
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { selectedTab = 0 },
            text = { Text("Volume", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (selectedTab == 0) Acid else OffWhite.copy(alpha = 0.5f))) }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { selectedTab = 1 },
            text = { Text("Big 3", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (selectedTab == 1) Acid else OffWhite.copy(alpha = 0.5f))) }
        )
        Tab(
            selected = selectedTab == 2,
            onClick = { selectedTab = 2 },
            text = { Text("P4P", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (selectedTab == 2) Acid else OffWhite.copy(alpha = 0.5f))) }
        )
    }

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
        itemsIndexed(sortedUsers) { index, user ->
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isLive = user.liveUpdatedAt != null && (System.currentTimeMillis() - user.liveUpdatedAt.time < 2 * 60 * 60 * 1000)
                        Text(
                            text = user.name,
                            style = Typography.titleLarge.copy(color = if (isLive) Color.Red else OffWhite, fontWeight = FontWeight.Bold)
                        )
                    }
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Total Volume",
                                style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.6f))
                            )
                        }
                        1 -> {
                            Text(
                                text = "B: ${user.benchPr} | S: ${user.squatPr} | D: ${user.deadliftPr}",
                                style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.6f))
                            )
                        }
                        else -> {
                            Text(
                                text = "BW: ${if (user.bodyWeight > 0) user.bodyWeight else "?"} KG",
                                style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.6f))
                            )
                        }
                    }
                }
                
                // Score
                val score = when (selectedTab) {
                    0 -> "${user.xp} KG"
                    1 -> "${user.benchPr + user.squatPr + user.deadliftPr} KG"
                    else -> if (user.bodyWeight > 0) String.format("%.2fx", user.xp / user.bodyWeight) else "0.00x"
                }
                Text(
                    text = score,
                    style = Typography.titleMedium.copy(color = Acid, fontWeight = FontWeight.Black)
                )
            }
        }
    }
}
}

@Composable
fun StalkBottomSheetContent(user: com.gymtracker.network.LeaderboardUser) {
    var stalkTab by remember { mutableStateOf(0) }
    
    val isLive = user.liveUpdatedAt != null && (System.currentTimeMillis() - user.liveUpdatedAt.time < 2 * 60 * 60 * 1000)
    val workoutJsonToUse = if (isLive && !user.liveWorkoutJson.isNullOrEmpty()) user.liveWorkoutJson else user.latestWorkoutJson

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = "${user.name}'s Profile",
                style = Typography.titleLarge.copy(color = if (isLive) Color.Red else Acid, fontWeight = FontWeight.Black)
            )
        }
        
        val nowPlayingTrack = com.gymtracker.media.NowPlayingTrack.fromJsonString(user.nowPlayingJson)
        if (nowPlayingTrack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2B2E42))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "blud is listening to ",
                    color = OffWhite.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    maxLines = 1
                )
                Text(
                    text = nowPlayingTrack.title,
                    color = OffWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
        } else {
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        TabRow(
            selectedTabIndex = stalkTab,
            containerColor = Color.Transparent,
            contentColor = Acid,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[stalkTab]),
                    color = Acid,
                    height = 3.dp
                )
            },
            divider = { Divider(color = Muted, thickness = 1.dp) },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Tab(selected = stalkTab == 0, onClick = { stalkTab = 0 }) {
                Text("Workout", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (stalkTab == 0) Acid else OffWhite.copy(alpha = 0.5f)), modifier = Modifier.padding(bottom = 8.dp))
            }
            Tab(selected = stalkTab == 1, onClick = { stalkTab = 1 }) {
                Text("PRs", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (stalkTab == 1) Acid else OffWhite.copy(alpha = 0.5f)), modifier = Modifier.padding(bottom = 8.dp))
            }
            Tab(selected = stalkTab == 2, onClick = { stalkTab = 2 }) {
                Text("Stats", style = Typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = if (stalkTab == 2) Acid else OffWhite.copy(alpha = 0.5f)), modifier = Modifier.padding(bottom = 8.dp))
            }
        }

        when (stalkTab) {
            0 -> {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    try {
                        val jsonStr = workoutJsonToUse ?: "[]"
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
                    item { Spacer(modifier = Modifier.height(48.dp)) }
                }
            }
            1 -> {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PrCard("Bench Press", user.benchPr)
                    PrCard("Squat", user.squatPr)
                    PrCard("Deadlift", user.deadliftPr)
                }
            }
            2 -> {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard("Total Volume", "${user.xp} KG")
                    StatCard("Bodyweight", if (user.bodyWeight > 0) "${user.bodyWeight} KG" else "Not set")
                    val p4p = if (user.bodyWeight > 0) String.format("%.2fx", user.xp / user.bodyWeight) else "0.00x"
                    StatCard("P4P Score", p4p)
                }
            }
        }
    }
}

@Composable
fun PrCard(liftName: String, prWeight: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Dim)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(liftName, style = Typography.titleMedium.copy(color = OffWhite, fontWeight = FontWeight.Medium))
        Text("${prWeight} KG", style = Typography.titleLarge.copy(color = Acid, fontWeight = FontWeight.Black))
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Dim)
            .padding(16.dp)
    ) {
        Text(label, style = Typography.labelMedium.copy(color = Color.Gray))
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, style = Typography.headlineSmall.copy(color = OffWhite, fontWeight = FontWeight.Bold))
    }
}
