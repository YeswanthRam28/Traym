package com.gymtracker.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.gymtracker.auth.SessionManager
import com.gymtracker.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo / Title
        Text(
            text = "TRAYM",
            style = Typography.displayLarge.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Acid
            ),
            letterSpacing = 2.sp
        )
        
        Text(
            text = "Your AI Coach & Log",
            style = Typography.titleMedium.copy(
                color = OffWhite.copy(alpha = 0.6f)
            )
        )
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Google Sign In Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Dim)
                .border(1.dp, OffWhite.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .clickable(enabled = !isLoading) {
                    coroutineScope.launch {
                        isLoading = true
                        errorMessage = null
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId("273285141091-jrbvb6r9g3pkj6fhttsm2ir6roku18fo.apps.googleusercontent.com")
                                .setAutoSelectEnabled(true)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(
                                request = request,
                                context = context
                            )

                            val credential = result.credential
                            if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                
                                SessionManager.setLoggedIn(
                                    loggedIn = true,
                                    token = googleIdTokenCredential.idToken,
                                    userName = googleIdTokenCredential.displayName ?: "Athlete",
                                    userId = googleIdTokenCredential.id,
                                    profilePicUrl = googleIdTokenCredential.profilePictureUri?.toString()
                                )
                                
                                onLoginSuccess()
                            } else {
                                errorMessage = "Unexpected credential type."
                            }
                        } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                            // User cancelled
                        } catch (e: Exception) {
                            errorMessage = e.localizedMessage ?: "Authentication failed"
                        } finally {
                            isLoading = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Acid, modifier = Modifier.size(24.dp))
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "G",
                        style = Typography.titleLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Text(
                        text = "Continue with Google",
                        style = Typography.titleMedium.copy(
                            color = OffWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
        
        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = errorMessage!!,
                style = Typography.bodyMedium.copy(color = Color.Red),
                textAlign = TextAlign.Center
            )
        }
    }
}
