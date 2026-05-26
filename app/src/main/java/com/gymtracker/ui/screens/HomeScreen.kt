package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.components.NavItem
import com.gymtracker.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.viewmodels.HomeViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person

data class PrItem(val name: String, val weight: String)

data class HomeUiState(
    val volume: String = "0",
    val todayWorkoutTitle: String = "NO SCHEDULED WORKOUT",
    val todayWorkoutDesc: String = "",
    val recentPrs: List<PrItem> = emptyList()
)

@Composable
fun HomeScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == "home") {
            viewModel.refreshDashboard()
        }
    }

    Scaffold(
        containerColor = AppBlack,
        bottomBar = {
            NavBar(
currentRoute = currentRoute,
                onNavigate = onNavigate
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Tokens.PaddingHorizontal)
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY",
                    style = Typography.displaySmall.copy(color = OffWhite, fontWeight = FontWeight.Black)
                )
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Dim, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigate("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "My Profile",
                        tint = Acid,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Hero Stat Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Dim),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WORKOUT VOLUME",
                        style = Typography.labelLarge.copy(color = OffWhite.copy(alpha = 0.6f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.volume,
                        style = Typography.displayLarge.copy(
                            color = Acid,
                            fontSize = 64.sp,
                            lineHeight = 64.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Today's Workout Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Acid),
                shape = RoundedCornerShape(0.dp),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigate("workout") }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = uiState.todayWorkoutTitle,
                        style = Typography.headlineMedium.copy(color = AppBlack)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.todayWorkoutDesc,
                        style = Typography.bodyMedium.copy(color = AppBlack.copy(alpha = 0.8f))
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "RECENT PRs",
                style = Typography.labelLarge.copy(color = OffWhite)
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.recentPrs) { pr ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = AppBlack),
                        shape = RoundedCornerShape(0.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .height(Tokens.CardHeight)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = pr.name,
                                style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.6f))
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = pr.weight,
                                style = Typography.titleLarge.copy(color = OffWhite)
                            )
                        }
                    }
                }
            }
        }
    }
}
