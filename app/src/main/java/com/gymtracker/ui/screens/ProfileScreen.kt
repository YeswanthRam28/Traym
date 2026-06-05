package com.gymtracker.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.auth.SessionManager
import com.gymtracker.network.NotionSyncManager
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.theme.*
import com.gymtracker.ui.viewmodels.ProfileViewModel
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color

data class ProfileUiState(
    val userName: String = "",
    val streak: String = "0",
    val workouts: String = "0",
    val kgLifted: String = "0",
    val philosophy: String = ""
)

@Composable
fun ProfileScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.uploadExportFile(context, it) }
    }

    LaunchedEffect(errorMessage, successMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == "profile") {
            viewModel.fetchProfile()
        }
    }

    var showNotionDialog by remember { mutableStateOf(false) }
    var showConnectedAppsDialog by remember { mutableStateOf(false) }

    val settingsItems = listOf(
        "Account Details", "Training Preferences", "Connected Apps", 
        "Data Export", "Notifications", "Clear Local Data"
    )

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Tokens.PaddingHorizontal, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PROFILE",
                        style = Typography.displaySmall.copy(color = OffWhite)
                    )
                    if (isLoading) {
                        CircularProgressIndicator(color = Acid, modifier = Modifier.size(24.dp))
                    }
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Name
            Text(
                text = uiState.userName,
                style = Typography.displaySmall.copy(color = Acid, fontSize = 36.sp),
                modifier = Modifier.padding(horizontal = Tokens.PaddingHorizontal)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Three Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.PaddingHorizontal),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "STREAK", style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uiState.streak, style = Typography.titleLarge.copy(color = OffWhite))
                }
                
                Divider(color = Muted, modifier = Modifier.width(1.dp).height(40.dp))
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "WORKOUTS", style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uiState.workouts, style = Typography.titleLarge.copy(color = OffWhite))
                }
                
                Divider(color = Muted, modifier = Modifier.width(1.dp).height(40.dp))
                
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "KG LIFTED", style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = uiState.kgLifted, style = Typography.titleLarge.copy(color = OffWhite))
                }
            }
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Training Philosophy Card
            Surface(
                color = Dim,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.PaddingHorizontal)
                    .height(100.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "PHILOSOPHY", style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.philosophy,
                        style = Typography.displaySmall.copy(color = Acid, fontSize = 24.sp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Settings List
            Column(modifier = Modifier.fillMaxWidth()) {
                settingsItems.forEach { item ->
                    Divider(color = Muted, thickness = 0.5.dp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                when (item) {
                                    "Connected Apps" -> {
                                        showConnectedAppsDialog = true
                                    }
                                    "Data Export" -> {
                                        Toast.makeText(context, "Data Export coming soon", Toast.LENGTH_SHORT).show()
                                    }
                                    "Clear Local Data" -> {
                                        coroutineScope.launch {
                                            SessionManager.clearLocalData()
                                            Toast.makeText(context, "Local data cleared!", Toast.LENGTH_SHORT).show()
                                            onNavigate("home")
                                        }
                                    }
                                    else -> {
                                        Toast.makeText(context, "$item coming soon", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(horizontal = Tokens.PaddingHorizontal),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = item, style = Typography.bodyMedium.copy(color = OffWhite))
                        Text(text = "→", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.5f)))
                    }
                }
                Divider(color = Muted, thickness = 0.5.dp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showNotionDialog) {
        var token by remember { mutableStateOf(SessionManager.getNotionToken()) }
        var autoSyncEnabled by remember { mutableStateOf(SessionManager.isNotionSyncEnabled()) }
        var syncStatus by remember { mutableStateOf("") }
        var isSyncing by remember { mutableStateOf(false) }
        var isConnected by remember { mutableStateOf(SessionManager.getNotionToken().isNotEmpty()) }
        var showAdvanced by remember { mutableStateOf(false) }
        var advancedToken by remember { mutableStateOf(SessionManager.getNotionToken()) }
        var advancedDbId by remember { mutableStateOf(SessionManager.getNotionDatabaseId()) }
        val notionSetupStatus by viewModel.notionSetupStatus.collectAsState()

        LaunchedEffect(notionSetupStatus) {
            if (notionSetupStatus == null) {
                isConnected = SessionManager.getNotionToken().isNotEmpty()
            }
        }

        AlertDialog(
            onDismissRequest = { if (!isSyncing && notionSetupStatus == null) showNotionDialog = false },
            title = {
                Text(
                    text = "NOTION SYNC CONFIG",
                    style = Typography.headlineMedium.copy(color = Acid)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "1-Click setup to automatically back up and fetch your workouts.",
                        style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.7f))
                    )
                    
                    if (notionSetupStatus != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.material3.CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Acid,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = notionSetupStatus ?: "",
                                style = Typography.bodySmall.copy(color = Acid)
                            )
                        }
                    } else if (isConnected) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "✓ Connected to Notion",
                                style = Typography.bodyMedium.copy(color = Acid)
                            )
                            TextButton(onClick = {
                                SessionManager.saveNotionConfig("", "", false)
                                isConnected = false
                            }) {
                                Text("Disconnect", color = Color.Red.copy(alpha = 0.8f))
                            }
                        }
                    } else if (showAdvanced) {
                        TextField(
                            value = advancedToken,
                            onValueChange = { advancedToken = it },
                            label = { Text("Integration Token") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                cursorColor = Acid
                            ),
                            textStyle = Typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        TextField(
                            value = advancedDbId,
                            onValueChange = { advancedDbId = it },
                            label = { Text("Database ID") },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedTextColor = OffWhite,
                                unfocusedTextColor = OffWhite,
                                cursorColor = Acid
                            ),
                            textStyle = Typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                SessionManager.saveNotionConfig(advancedToken, advancedDbId, autoSyncEnabled)
                                isConnected = advancedToken.isNotEmpty()
                                showAdvanced = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Dim, contentColor = Acid),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE MANUALLY", style = Typography.labelLarge)
                        }
                    } else {
                        Button(
                            onClick = {
                                val clientId = com.gymtracker.BuildConfig.NOTION_CLIENT_ID
                                val redirectUri = java.net.URLEncoder.encode(com.gymtracker.BuildConfig.NOTION_REDIRECT_URI, "UTF-8")
                                val url = "https://api.notion.com/v1/oauth/authorize?client_id=$clientId&response_type=code&owner=user&redirect_uri=$redirectUri"
                                val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                                    .setShowTitle(true)
                                    .build()
                                customTabsIntent.launchUrl(context, android.net.Uri.parse(url))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = AppBlack),
                            shape = RoundedCornerShape(0.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CONNECT WITH NOTION", style = Typography.labelLarge)
                        }
                        TextButton(
                            onClick = { showAdvanced = true },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Advanced Setup", color = OffWhite.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    }
                    

                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = autoSyncEnabled,
                            onCheckedChange = { autoSyncEnabled = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Acid,
                                uncheckedColor = Muted,
                                checkmarkColor = AppBlack
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Auto-sync completed workouts",
                            style = Typography.bodyMedium.copy(color = OffWhite)
                        )
                    }
                    
                    if (syncStatus.isNotEmpty()) {
                        Text(
                            text = syncStatus,
                            style = Typography.bodySmall.copy(color = if (syncStatus.startsWith("Sync error")) Color.Red else Acid)
                        )
                    }
                }
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            isSyncing = true
                            syncStatus = "Synchronizing database..."
                            SessionManager.saveNotionConfig(SessionManager.getNotionToken(), SessionManager.getNotionDatabaseId(), autoSyncEnabled)
                            NotionSyncManager.syncWithNotion(
                                onSuccess = { msg ->
                                    coroutineScope.launch {
                                        isSyncing = false
                                        syncStatus = msg
                                        viewModel.fetchProfile()
                                    }
                                },
                                onError = { err ->
                                    coroutineScope.launch {
                                        isSyncing = false
                                        syncStatus = err
                                    }
                                }
                            )
                        },
                        enabled = !isSyncing && isConnected
                    ) {
                        Text("SYNC NOW", color = Acid)
                    }
                    Button(
                        onClick = {
                            if (isConnected) {
                                SessionManager.saveNotionConfig(SessionManager.getNotionToken(), SessionManager.getNotionDatabaseId(), autoSyncEnabled)
                            }
                            showNotionDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Acid, contentColor = AppBlack),
                        shape = RoundedCornerShape(0.dp),
                        enabled = !isSyncing
                    ) {
                        Text("SAVE")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showNotionDialog = false },
                    enabled = !isSyncing
                ) {
                    Text("CLOSE", color = OffWhite)
                }
            },
            containerColor = Dim,
            shape = RoundedCornerShape(0.dp)
        )
    }

    if (showConnectedAppsDialog) {
        var isGoogleConnected by remember { mutableStateOf(SessionManager.authToken != null) }
        
        AlertDialog(
            onDismissRequest = { showConnectedAppsDialog = false },
            title = {
                Text(
                    text = "CONNECTED APPS",
                    style = Typography.headlineMedium.copy(color = Acid)
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Google Auth Option
                    Surface(
                        color = if (isGoogleConnected) Acid.copy(alpha = 0.1f) else AppBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isGoogleConnected) Acid else Muted),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (isGoogleConnected) {
                                    // Disconnect Google
                                    SessionManager.authToken = null
                                    isGoogleConnected = false
                                    Toast.makeText(context, "Google Disconnected.", Toast.LENGTH_SHORT).show()
                                } else {
                                    // We will let them sign in through LoginScreen for now
                                    Toast.makeText(context, "Please sign out and sign in from the main screen to connect Google.", Toast.LENGTH_LONG).show()
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Google Account", style = Typography.bodyLarge.copy(color = OffWhite))
                                Text(
                                    text = if (isGoogleConnected) "Connected" else "Not connected",
                                    style = Typography.bodySmall.copy(color = if (isGoogleConnected) Acid else OffWhite.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                    
                    // Notion Option
                    Surface(
                        color = AppBlack,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Muted),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showConnectedAppsDialog = false
                                showNotionDialog = true
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Notion Sync", style = Typography.bodyLarge.copy(color = OffWhite))
                                Text("Configure database sync", style = Typography.bodySmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConnectedAppsDialog = false }) {
                    Text("DONE", color = Acid)
                }
            },
            containerColor = Dim,
            shape = RoundedCornerShape(0.dp)
        )
    }
}
