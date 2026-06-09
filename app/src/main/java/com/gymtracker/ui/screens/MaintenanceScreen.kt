package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.R
import com.gymtracker.ui.theme.AppBlack
import com.gymtracker.ui.theme.Acid
import com.gymtracker.ui.theme.AnybodyFamily

@Composable
fun MaintenanceScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBlack),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = "SERVER LOCK",
                fontFamily = AnybodyFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Acid,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "TRAYM is currently undergoing global maintenance. Our servers are locked down while we deploy an upgrade.",
                color = androidx.compose.ui.graphics.Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Please check back later.",
                color = androidx.compose.ui.graphics.Color.Gray,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
