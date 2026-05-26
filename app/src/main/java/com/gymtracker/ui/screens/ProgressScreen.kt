package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.components.NavItem
import com.gymtracker.ui.theme.*
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberEndAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.textComponent
import com.patrykandpatrick.vico.core.chart.values.AxisValuesOverrider
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.viewmodels.ProgressViewModel

data class StatItem(val label: String, val value: String)

data class ProgressUiState(
    val strengthTrendPoints: List<Float> = emptyList(),
    val plateauAlert: String = "",
    val stats: List<StatItem> = emptyList(),
    val isMuscleMapActive: Boolean = false
)

@Composable
fun ProgressScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: ProgressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(currentRoute) {
        if (currentRoute == "progress") {
            viewModel.fetchProgressData()
        }
    }

    val chartEntryModel = remember(uiState.strengthTrendPoints) {
        if (uiState.strengthTrendPoints.isNotEmpty()) {
            entryModelOf(*uiState.strengthTrendPoints.toTypedArray())
        } else {
            null
        }
    }
    
    val bottomAxisValueFormatter = AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
        val dates = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul")
        dates.getOrNull(value.toInt()) ?: ""
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PROGRESS / STATS",
                        style = Typography.displaySmall.copy(color = OffWhite)
                    )
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
            Spacer(modifier = Modifier.height(24.dp))
            
            // Chart Section
            Column(modifier = Modifier.padding(horizontal = Tokens.PaddingHorizontal)) {
                Text(text = "STRENGTH TREND", style = Typography.labelLarge.copy(color = OffWhite))
                Spacer(modifier = Modifier.height(16.dp))
                
                if (chartEntryModel != null) {
                    Chart(
                        chart = lineChart(
                            lines = listOf(
                                lineSpec(
                                    lineColor = Acid,
                                    lineBackgroundShader = null
                                )
                            )
                        ),
                        model = chartEntryModel,
                        bottomAxis = rememberBottomAxis(
                            valueFormatter = bottomAxisValueFormatter,
                            guideline = null,
                            label = textComponent(
                                color = OffWhite,
                                textSize = 12.sp,
                            )
                        ),
                        endAxis = rememberEndAxis(
                            guideline = null,
                            label = textComponent(
                                color = OffWhite,
                                textSize = 12.sp,
                            )
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Plateau Alert Card
                if (uiState.plateauAlert.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Dim),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                drawLine(
                                    color = Acid,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "⚠️", modifier = Modifier.padding(end = 12.dp))
                            Text(
                                text = uiState.plateauAlert,
                                style = Typography.labelLarge.copy(color = OffWhite)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats Grid (LazyRow)
            LazyRow(
                contentPadding = PaddingValues(horizontal = Tokens.PaddingHorizontal),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(uiState.stats.size) { index ->
                    val stat = uiState.stats[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Dim),
                        shape = RoundedCornerShape(0.dp),
                        modifier = Modifier
                            .width(140.dp)
                            .height(100.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(text = stat.label, style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)))
                            Text(text = stat.value, style = Typography.titleLarge.copy(color = OffWhite))
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Muscle Map
            Column(modifier = Modifier.padding(horizontal = Tokens.PaddingHorizontal)) {
                Text(text = "MUSCLE RECOVERY", style = Typography.labelLarge.copy(color = OffWhite))
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Dim)
                        .drawBehind {
                            // Dummy silhouette path
                            val path = Path().apply {
                                val cx = size.width / 2
                                // Head
                                addOval(androidx.compose.ui.geometry.Rect(cx - 20f, 20f, cx + 20f, 60f))
                                // Torso
                                moveTo(cx - 30f, 70f)
                                lineTo(cx + 30f, 70f)
                                lineTo(cx + 25f, 150f)
                                lineTo(cx - 25f, 150f)
                                close()
                                // Left Arm
                                moveTo(cx - 30f, 70f)
                                lineTo(cx - 60f, 120f)
                                lineTo(cx - 50f, 125f)
                                lineTo(cx - 25f, 80f)
                                close()
                                // Right Arm
                                moveTo(cx + 30f, 70f)
                                lineTo(cx + 60f, 120f)
                                lineTo(cx + 50f, 125f)
                                lineTo(cx + 25f, 80f)
                                close()
                            }
                            drawPath(path, color = Muted, style = Stroke(width = 2.dp.toPx()))
                        }
                ) {
                    // Overlay colored Box region for chest
                    if (uiState.isMuscleMapActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = 80.dp)
                                .size(width = 40.dp, height = 20.dp)
                                .background(Acid.copy(alpha = 0.5f))
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
