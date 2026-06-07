package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.gymtracker.ui.viewmodels.AiChatViewModel
import com.gymtracker.ui.viewmodels.ChatMessage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue

data class PrItem(val name: String, val weight: String, val isBigThree: Boolean = false)

data class HomeUiState(
    val volume: String = "0",
    val todayWorkoutTitle: String = "NO SCHEDULED WORKOUT",
    val todayWorkoutDesc: String = "",
    val recentPrs: List<PrItem> = emptyList(),
    val profilePicUrl: String? = null
)

@Composable
fun HomeScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
    aiChatViewModel: AiChatViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var showAiChat by remember { mutableStateOf(false) }
    var chatInputText by remember { mutableStateOf("") }
    val chatMessages = aiChatViewModel.messages.reversed()

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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    aiChatViewModel.connect()
                    showAiChat = true 
                },
                containerColor = Acid,
                contentColor = AppBlack,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Person, contentDescription = "AI Coach")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Tokens.PaddingHorizontal)
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
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
                    if (uiState.profilePicUrl != null) {
                        coil.compose.AsyncImage(
                            model = uiState.profilePicUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "My Profile",
                            tint = Acid,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val bigThreePrs = uiState.recentPrs.filter { it.isBigThree }
            val recentPrs = uiState.recentPrs.filter { !it.isBigThree }

            if (bigThreePrs.isNotEmpty()) {
                Text(
                    text = "BIG 3 PRs",
                    style = Typography.labelLarge.copy(color = OffWhite)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(bigThreePrs) { pr ->
                        PrCard(pr)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (recentPrs.isNotEmpty()) {
                Text(
                    text = "RECENT PRs",
                    style = Typography.labelLarge.copy(color = OffWhite)
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(recentPrs) { pr ->
                        PrCard(pr)
                    }
                }
            }
        }
    }
    
    if (showAiChat) {
        @OptIn(ExperimentalMaterial3Api::class)
        ModalBottomSheet(
            onDismissRequest = { showAiChat = false },
            containerColor = AppBlack,
            scrimColor = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.PaddingHorizontal, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI COACH",
                        style = Typography.displaySmall.copy(color = OffWhite)
                    )
                }
                Divider(color = Muted, thickness = 0.5.dp)
                
                // Messages List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = Tokens.PaddingHorizontal),
                    reverseLayout = true,
                    contentPadding = PaddingValues(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(chatMessages) { message ->
                        if (message.isUser) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Surface(
                                    color = Acid,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    Text(
                                        text = message.text,
                                        style = Typography.bodyMedium.copy(color = AppBlack),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(0.85f),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Acid, RoundedCornerShape(12.dp))
                                        .drawBehind {
                                            val path = Path().apply {
                                                moveTo(size.width * 0.3f, size.height * 0.4f)
                                                lineTo(size.width * 0.7f, size.height * 0.4f)
                                                moveTo(size.width * 0.3f, size.height * 0.6f)
                                                lineTo(size.width * 0.7f, size.height * 0.6f)
                                            }
                                            drawPath(path, AppBlack, style = Stroke(width = 2.dp.toPx()))
                                        }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = Dim,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = message.text,
                                        style = Typography.bodyMedium.copy(color = OffWhite),
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.PaddingHorizontal, vertical = 8.dp)
                        .navigationBarsPadding()
                        .height(Tokens.ButtonHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = chatInputText,
                        onValueChange = { chatInputText = it },
                        placeholder = { Text(text = "Ask anything...", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.5f))) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Dim,
                            unfocusedContainerColor = Dim,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Acid,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            aiChatViewModel.sendMessage(chatInputText)
                            chatInputText = ""
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = AppBlack),
                        modifier = Modifier.fillMaxHeight(),
                        enabled = chatInputText.isNotBlank()
                    ) {
                        Text("↑", style = Typography.headlineMedium)
                    }
                }
            }
        }
    }
}

@Composable
fun PrCard(pr: PrItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppBlack),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier
            .width(180.dp)
            .height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = pr.name,
                style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.6f)),
                maxLines = 3,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = pr.weight,
                style = Typography.titleLarge.copy(color = OffWhite)
            )
        }
    }
}
