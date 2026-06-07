package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Acid
import com.gymtracker.ui.theme.AppBlack
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.Muted
import com.gymtracker.ui.theme.OffWhite
import com.gymtracker.ui.theme.Typography
import com.gymtracker.data.ProgressStats
import com.gymtracker.ui.viewmodels.ProgressViewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.gymtracker.R
import androidx.compose.ui.layout.ContentScale

val PieColors = listOf(
    Color(0xFFE5FF00), // Acid
    Color(0xFF00FFCC), // Teal
    Color(0xFFFF007F), // Pink
    Color(0xFF9D00FF), // Purple
    Color(0xFFFF8C00), // Orange
    Color(0xFF00BFFF), // Light Blue
    Color(0xFF32CD32)  // Lime Green
)

val FrontMusclePoints = mapOf(
    "Chest" to listOf(Offset(0.5f, 0.28f)),
    "Shoulders" to listOf(Offset(0.35f, 0.25f), Offset(0.65f, 0.25f)),
    "Biceps" to listOf(Offset(0.25f, 0.35f), Offset(0.75f, 0.35f)),
    "Abs" to listOf(Offset(0.5f, 0.40f)),
    "Core" to listOf(Offset(0.5f, 0.40f)),
    "Legs" to listOf(Offset(0.40f, 0.65f), Offset(0.60f, 0.65f)),
    "Quads" to listOf(Offset(0.40f, 0.65f), Offset(0.60f, 0.65f)),
    "Calves" to listOf(Offset(0.38f, 0.85f), Offset(0.62f, 0.85f)),
    "Arms" to listOf(Offset(0.25f, 0.35f), Offset(0.75f, 0.35f))
)

val BackMusclePoints = mapOf(
    "Back" to listOf(Offset(0.5f, 0.35f)),
    "Traps" to listOf(Offset(0.5f, 0.20f)),
    "Triceps" to listOf(Offset(0.20f, 0.35f), Offset(0.80f, 0.35f)),
    "Glutes" to listOf(Offset(0.45f, 0.55f), Offset(0.55f, 0.55f)),
    "Hamstrings" to listOf(Offset(0.40f, 0.70f), Offset(0.60f, 0.70f)),
    "Legs" to listOf(Offset(0.40f, 0.70f), Offset(0.60f, 0.70f)),
    "Arms" to listOf(Offset(0.20f, 0.35f), Offset(0.80f, 0.35f))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatDetailScreen(
    type: String,
    onBack: () -> Unit,
    viewModel: ProgressViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val stats = uiState

    val title = when(type) {
        "muscles" -> "Muscle Groups"
        "exercises" -> "Main Exercises"
        "leaderboard" -> "Leaderboard"
        "monthly" -> "Monthly Report"
        "piechart" -> "Muscle Chart"
        "body" -> "Body Distribution"
        else -> "Statistics"
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            TopAppBar(
                title = { Text(title, style = Typography.titleLarge.copy(color = OffWhite)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OffWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBlack)
            )
        }
    ) { paddingValues ->
        if (stats == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Acid)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            when (type) {
                "muscles" -> {
                    itemsIndexed(stats.topMusclesBySets) { index, item ->
                        StatRowItem(rank = index + 1, name = item.first, value = "${item.second} sets")
                    }
                }
                "exercises" -> {
                    itemsIndexed(stats.topExercisesBySets) { index, item ->
                        StatRowItem(rank = index + 1, name = item.first, value = "${item.second} sets")
                    }
                }
                "monthly" -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Volume This Month", style = Typography.bodyLarge.copy(color = OffWhite.copy(alpha=0.6f)))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(String.format("%.0f KG", stats.monthlyVolumeKg), style = Typography.displayMedium.copy(color = Acid, fontWeight = FontWeight.Bold))
                        }
                    }
                }
                "leaderboard" -> {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No PRs logged yet.", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha=0.5f)))
                        }
                    }
                }
                "piechart" -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val totalSets = stats.topMusclesBySets.sumOf { it.second }.toFloat()
                            if (totalSets == 0f) {
                                Text("No muscle data found.", style = Typography.bodyMedium.copy(color = OffWhite))
                            } else {
                                Box(modifier = Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        var currentStartAngle = -90f
                                        stats.topMusclesBySets.forEachIndexed { index, pair ->
                                            val sweepAngle = (pair.second.toFloat() / totalSets) * 360f
                                            val color = PieColors[index % PieColors.size]
                                            drawArc(
                                                color = color,
                                                startAngle = currentStartAngle,
                                                sweepAngle = sweepAngle,
                                                useCenter = false,
                                                size = Size(size.width, size.height),
                                                style = Stroke(width = 80f)
                                            )
                                            currentStartAngle += sweepAngle
                                        }
                                    }
                                    Text("Muscles", style = Typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold))
                                }
                                Spacer(modifier = Modifier.height(48.dp))
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                                    stats.topMusclesBySets.forEachIndexed { index, pair ->
                                        val color = PieColors[index % PieColors.size]
                                        val percentage = ((pair.second.toFloat() / totalSets) * 100).toInt()
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).background(color))
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(pair.first, style = Typography.bodyMedium.copy(color = OffWhite), modifier = Modifier.weight(1f))
                                            Text("$percentage%", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha=0.7f)))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "body" -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val maxSets = stats.topMusclesBySets.maxOfOrNull { it.second } ?: 1
                            Row(
                                modifier = Modifier.fillMaxWidth().height(300.dp).padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BodyHeatmapImage(
                                    drawableRes = R.drawable.human_body_frontal,
                                    contentDescription = "Front Body",
                                    musclePoints = FrontMusclePoints,
                                    stats = stats,
                                    maxSets = maxSets,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                                BodyHeatmapImage(
                                    drawableRes = R.drawable.human_body,
                                    contentDescription = "Back Body",
                                    musclePoints = BackMusclePoints,
                                    stats = stats,
                                    maxSets = maxSets,
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            if (stats.topMusclesBySets.isEmpty()) {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    Text("No muscle data found.", style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha=0.5f)))
                                }
                            } else {
                                stats.topMusclesBySets.forEachIndexed { index, pair ->
                                    val fraction = pair.second.toFloat() / maxSets.toFloat()
                                    val color = PieColors[index % PieColors.size]
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = pair.first,
                                            modifier = Modifier.weight(0.3f),
                                            style = Typography.labelMedium.copy(color = OffWhite, fontWeight = FontWeight.Bold)
                                        )
                                        Box(modifier = Modifier.weight(0.5f).height(12.dp).background(Dim, RoundedCornerShape(6.dp))) {
                                            Box(modifier = Modifier.fillMaxWidth(fraction).height(12.dp).background(color, RoundedCornerShape(6.dp)))
                                        }
                                        Text(
                                            text = "${pair.second} sets",
                                            modifier = Modifier.weight(0.2f),
                                            textAlign = TextAlign.End,
                                            style = Typography.labelSmall.copy(color = Muted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRowItem(rank: Int, name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Dim)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "#$rank",
            style = Typography.titleMedium.copy(color = Acid, fontWeight = FontWeight.Bold),
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = name,
            style = Typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Medium),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.7f))
        )
    }
}

@Composable
fun BodyHeatmapImage(
    drawableRes: Int,
    contentDescription: String,
    musclePoints: Map<String, List<Offset>>,
    stats: ProgressStats,
    maxSets: Int,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(id = drawableRes),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            stats.topMusclesBySets.forEachIndexed { index, pair ->
                val fraction = pair.second.toFloat() / maxSets.toFloat()
                val color = PieColors[index % PieColors.size]
                
                val points = musclePoints[pair.first]
                points?.forEach { relativeOffset ->
                    val x = relativeOffset.x * canvasWidth
                    val y = relativeOffset.y * canvasHeight
                    
                    val maxRadius = canvasWidth * 0.20f
                    val radius = (canvasWidth * 0.05f) + (fraction * maxRadius)
                    
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.9f), color.copy(alpha = 0f)),
                            center = Offset(x, y),
                            radius = radius
                        ),
                        radius = radius,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
