package com.gymtracker.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.gymtracker.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.viewmodels.WorkoutSummaryViewModel

data class WorkoutSummaryUiState(
    val workoutTitle: String = "",
    val exercisesCount: String = "0",
    val duration: String = "0m",
    val volume: String = "0",
    val newPrsCount: String = "0",
    val prCalloutTitle: String = "",
    val prCalloutValue: String = ""
)

@Composable
fun WorkoutSummaryScreen(
    onDone: () -> Unit,
    viewModel: WorkoutSummaryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var showPrCallout by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.prCalloutTitle) {
        if (uiState.prCalloutTitle.isNotEmpty()) {
            delay(300)
            showPrCallout = true
        }
    }

    Scaffold(
        containerColor = Acid,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = Tokens.PaddingHorizontal, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "WORKOUT COMPLETE",
                    style = Typography.labelLarge.copy(color = AppBlack)
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(Tokens.PaddingHorizontal)
                    .padding(bottom = 32.dp)
            ) {
                Button(
                    onClick = onDone,
                    shape = RoundedCornerShape(0.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppBlack, contentColor = Acid),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Tokens.ButtonHeight)
                ) {
                    Text(text = "DONE", style = Typography.titleMedium)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Tokens.PaddingHorizontal)
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = uiState.workoutTitle,
                style = Typography.displayMedium.copy(color = AppBlack)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // 2x2 Stats Grid
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = uiState.exercisesCount, style = Typography.displaySmall.copy(color = AppBlack, fontSize = 32.sp))
                        Text(text = "EXERCISES", style = Typography.labelSmall.copy(color = AppBlack))
                    }
                    Divider(
                        color = AppBlack,
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(60.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(text = uiState.duration, style = Typography.displaySmall.copy(color = AppBlack, fontSize = 32.sp))
                        Text(text = "DURATION", style = Typography.labelSmall.copy(color = AppBlack))
                    }
                }
                
                Divider(color = AppBlack, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 16.dp))
                
                // Row 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = uiState.volume, style = Typography.displaySmall.copy(color = AppBlack, fontSize = 32.sp))
                        Text(text = "VOLUME (LBS)", style = Typography.labelSmall.copy(color = AppBlack))
                    }
                    Divider(
                        color = AppBlack,
                        modifier = Modifier
                            .width(0.5.dp)
                            .height(60.dp)
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(text = uiState.newPrsCount, style = Typography.displaySmall.copy(color = AppBlack, fontSize = 32.sp))
                        Text(text = "NEW PRs", style = Typography.labelSmall.copy(color = AppBlack))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // PR Callout
            AnimatedVisibility(
                visible = showPrCallout,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(500)
                )
            ) {
                Surface(
                    color = AppBlack,
                    shape = RoundedCornerShape(0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.prCalloutTitle,
                            style = Typography.labelLarge.copy(color = Acid)
                        )
                        Text(
                            text = uiState.prCalloutValue,
                            style = Typography.titleLarge.copy(color = Acid)
                        )
                    }
                }
            }
        }
    }
}
