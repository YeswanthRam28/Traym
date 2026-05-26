package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.components.NavItem
import com.gymtracker.ui.theme.*
import com.gymtracker.ui.viewmodels.AiChatViewModel

data class ChatMessage(val text: String, val isUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: AiChatViewModel = viewModel()
) {
    val messages = viewModel.messages.reversed()
    var inputText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.connect()
    }

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
                        text = "AI COACH",
                        style = Typography.displaySmall.copy(color = OffWhite)
                    )
                }
                Divider(color = Muted, thickness = 0.5.dp)
            }
        },
        bottomBar = {
            Column(modifier = Modifier.background(AppBlack)) {
                // Context Chips
                LazyRow(
                    contentPadding = PaddingValues(horizontal = Tokens.PaddingHorizontal, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Placeholder for future context chips
                }
                
                // Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Tokens.PaddingHorizontal, vertical = 8.dp)
                        .height(Tokens.ButtonHeight),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
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
                        shape = RoundedCornerShape(0.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        },
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = AppBlack),
                        modifier = Modifier.fillMaxHeight(),
                        enabled = inputText.isNotBlank()
                    ) {
                        Text("↑", style = Typography.headlineMedium)
                    }
                }
                
                NavBar(
currentRoute = currentRoute,
                    onNavigate = onNavigate
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = Tokens.PaddingHorizontal),
            reverseLayout = true,
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(messages) { message ->
                if (message.isUser) {
                    // User Message
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            color = Acid,
                            shape = RoundedCornerShape(0.dp),
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
                    // AI Message
                    Row(
                        modifier = Modifier.fillMaxWidth(0.85f),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        // AI Avatar
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(Acid)
                                .drawBehind {
                                    // Simple custom icon using Canvas
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
                            shape = RoundedCornerShape(0.dp)
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
    }
}
