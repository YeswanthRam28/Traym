package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.*
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.gymtracker.R

data class NavItem(
    val title: String,
    val iconResId: Int,
    val route: String
)

val MainNavItems = listOf(
    NavItem("HOME", R.drawable.ic_nav_home, "home"),
    NavItem("EXPLORE", R.drawable.ic_nav_explore, "explore"),
    NavItem("WORKOUT", R.drawable.ic_nav_workout, "workout"),
    NavItem("COACH", R.drawable.ic_nav_coach, "chat"),
    NavItem("PROGRESS", R.drawable.ic_nav_progress, "progress")
)

@Composable
fun NavBar(
    items: List<NavItem> = MainNavItems,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(Tokens.BottomNavHeight)
            .background(AppBlack) // Plain background
            .navigationBarsPadding(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            val contentColor = if (isSelected) Acid else OffWhite.copy(alpha = 0.5f)
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onNavigate(item.route) }
                    .drawBehind {
                        if (isSelected) {
                            val strokeWidth = 2.dp.toPx()
                            drawLine(
                                color = Acid,
                                start = Offset(0f, size.height - strokeWidth / 2),
                                end = Offset(size.width, size.height - strokeWidth / 2),
                                strokeWidth = strokeWidth
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // For icon, using a generic text fallback, ideally use Material Symbols
                    // The design might have used actual icons, but we'll use placeholder text/emoji
                    Icon(
                        painter = painterResource(id = item.iconResId),
                        contentDescription = item.title,
                        tint = contentColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.title,
                        color = contentColor,
                        style = Typography.labelMedium
                    )
                }
            }
        }
    }
}
