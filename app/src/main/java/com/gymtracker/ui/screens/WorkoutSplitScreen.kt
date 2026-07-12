package com.gymtracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.NavBar
import com.gymtracker.ui.components.NavItem
import com.gymtracker.ui.theme.*
import com.gymtracker.ui.viewmodels.PlannedExercise
import com.gymtracker.ui.viewmodels.WorkoutDay
import com.gymtracker.ui.viewmodels.WorkoutSplitViewModel
import com.gymtracker.ui.viewmodels.ActualSet
import org.burnoutcrew.reorderable.rememberReorderableLazyListState
import org.burnoutcrew.reorderable.reorderable
import org.burnoutcrew.reorderable.ReorderableItem
import org.burnoutcrew.reorderable.detectReorderAfterLongPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSplitScreen(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    viewModel: WorkoutSplitViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val history by viewModel.history.collectAsState()
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }
    var selectedExerciseNameForHistory by remember { mutableStateOf<String?>(null) }
    
    BackHandler(enabled = selectedDayIndex != null) {
        selectedDayIndex = null
    }

    LaunchedEffect(currentRoute) {
        if (currentRoute == "workout") {
            viewModel.loadSplit()
            viewModel.loadHistory()
        }
    }

    Scaffold(
        containerColor = AppBlack,
        topBar = {
            if (selectedDayIndex == null) {
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
                            text = if (isEditMode) "EDIT SPLIT" else "YOUR SPLIT",
                            style = Typography.displaySmall.copy(color = OffWhite)
                        )
                        TextButton(onClick = { viewModel.toggleEditMode() }) {
                            Text(
                                text = if (isEditMode) "DONE" else "EDIT",
                                style = Typography.labelLarge.copy(color = Acid)
                            )
                        }
                    }
                    Divider(color = Muted, thickness = 0.5.dp)
                }
            }
        },
        bottomBar = {
            if (selectedDayIndex == null && !isEditMode) {
                NavBar(currentRoute = currentRoute, onNavigate = onNavigate)
            }
        }
    ) { paddingValues ->
        if (selectedDayIndex == null) {
            val state = rememberReorderableLazyListState(onMove = { from, to ->
                viewModel.moveDay(from.index, to.index)
            })

            LazyColumn(
                state = state.listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = Tokens.PaddingHorizontal)
                    .reorderable(state),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                itemsIndexed(uiState.days, key = { _, day -> day.id }) { index, day ->
                    ReorderableItem(state, key = day.id) { isDragging ->
                        SplitDayCard(
                            day = day,
                            isEditMode = isEditMode,
                            onTitleChange = { viewModel.updateDayTitle(index, it) },
                            onNameChange = { viewModel.updateDayName(index, it) },
                            onRemove = { viewModel.removeDay(index) },
                            modifier = if (isEditMode) Modifier.detectReorderAfterLongPress(state) else Modifier,
                            onClick = { selectedDayIndex = index }
                        )
                    }
                }
                if (isEditMode) {
                    item {
                        Button(
                            onClick = { viewModel.addDay() },
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Dim)
                        ) {
                            Text("+ ADD DAY", color = Acid)
                        }
                    }
                }
            }
        } else {
            val dayIndex = selectedDayIndex!!
            DayDetailView(
                day = uiState.days[dayIndex],
                onBack = { selectedDayIndex = null },
                onAddExercise = { onNavigate("explore?dayIndex=$dayIndex") },
                onRemoveExercise = { viewModel.removeExercise(dayIndex, it) },
                onUpdateExercise = { exIndex, ex -> viewModel.updateExercise(dayIndex, exIndex, ex) },
                onMoveExercise = { from, to -> viewModel.moveExercise(dayIndex, from, to) },
                onToggleSet = { exIndex, setIndex -> viewModel.toggleSetCompleted(dayIndex, exIndex, setIndex) },
                onCycleSetType = { exIndex, setIndex -> viewModel.cycleSetType(dayIndex, exIndex, setIndex) },
                onUpdateSet = { exIndex, setIndex, weightInput, repsInput -> viewModel.updateActualSet(dayIndex, exIndex, setIndex, weightInput, repsInput) },
                onExerciseClick = { selectedExerciseNameForHistory = it },
                onSaveExercise = { exIndex -> viewModel.saveExercise(dayIndex, exIndex) },
                modifier = Modifier.padding(if (selectedDayIndex != null) PaddingValues(0.dp) else paddingValues).statusBarsPadding()
            )
        }
    }

    if (selectedExerciseNameForHistory != null) {
        val exerciseName = selectedExerciseNameForHistory!!
        val exerciseHistory = history.filter { it.title.equals(exerciseName, ignoreCase = true) }
        
        ModalBottomSheet(
            onDismissRequest = { selectedExerciseNameForHistory = null },
            containerColor = Dim,
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Tokens.PaddingHorizontal, vertical = 24.dp)
            ) {
                Text(
                    text = exerciseName.uppercase(),
                    style = Typography.displaySmall.copy(color = Acid, fontSize = 28.sp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "EXERCISE HISTORY",
                    style = Typography.labelLarge.copy(color = OffWhite.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (exerciseHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No history recorded for this exercise yet.",
                            style = Typography.bodyMedium.copy(color = OffWhite.copy(alpha = 0.6f))
                        )
                    }
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                    ) {
                        items(exerciseHistory.size) { idx ->
                            val entry = exerciseHistory[idx]
                            val dateDisplay = try {
                                val date = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).parse(entry.started_at)
                                java.text.SimpleDateFormat("MMMM d, yyyy - h:mm a", java.util.Locale.US).format(date)
                            } catch (e: Exception) {
                                entry.started_at
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = dateDisplay,
                                        style = Typography.bodyLarge.copy(color = OffWhite, fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${entry.total_sets ?: 0} SETS × ${entry.total_reps ?: 0} REPS",
                                        style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.5f))
                                    )
                                }
                                Text(
                                    text = "VOL: ${entry.total_volume_kg?.toInt() ?: 0} KG",
                                    style = Typography.titleMedium.copy(color = Acid)
                                )
                            }
                            
                            if (!entry.detailed_sets.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AppBlack.copy(alpha = 0.3f))
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    entry.detailed_sets.forEach { set ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "SET ${set.set_number}" + if (set.set_type != "Normal") " (${set.set_type})" else "",
                                                style = Typography.labelMedium.copy(color = OffWhite.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${set.weight_kg ?: 0.0} KG × ${set.reps ?: 0}",
                                                style = Typography.bodyMedium.copy(color = OffWhite)
                                            )
                                        }
                                    }
                                }
                            }
                            if (idx < exerciseHistory.size - 1) {
                                Divider(color = Muted.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(top = 12.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitDayCard(
    day: WorkoutDay,
    isEditMode: Boolean,
    onTitleChange: (String) -> Unit,
    onNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (day.splitTitle == "REST") Dim.copy(alpha = 0.3f) else Dim),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                IconButton(onClick = onRemove, modifier = Modifier.padding(end = 8.dp)) {
                    Text("✕", color = Color.Red, fontSize = 20.sp)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                if (isEditMode) {
                    TextField(
                        value = day.dayName,
                        onValueChange = onNameChange,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            cursorColor = Acid,
                            focusedTextColor = OffWhite.copy(alpha = 0.6f),
                            unfocusedTextColor = OffWhite.copy(alpha = 0.6f)
                        ),
                        textStyle = Typography.labelSmall,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    )
                } else {
                    Text(text = day.dayName, style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.6f)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (isEditMode) {
                    TextField(
                        value = day.splitTitle,
                        onValueChange = onTitleChange,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Acid.copy(alpha = 0.3f),
                            focusedIndicatorColor = Acid,
                            cursorColor = Acid,
                            focusedTextColor = OffWhite,
                            unfocusedTextColor = OffWhite
                        ),
                        textStyle = Typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = day.splitTitle,
                        style = Typography.headlineMedium.copy(
                            color = if (day.splitTitle == "REST") OffWhite.copy(alpha = 0.4f) else OffWhite,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (isEditMode) {
                Text(text = "☰", style = Typography.headlineMedium.copy(color = Muted), modifier = Modifier.padding(start = 16.dp))
            } else if (day.splitTitle != "REST") {
                Text(text = "→", style = Typography.headlineMedium.copy(color = Acid))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailView(
    day: WorkoutDay,
    onBack: () -> Unit,
    onAddExercise: () -> Unit,
    onRemoveExercise: (Int) -> Unit,
    onUpdateExercise: (Int, PlannedExercise) -> Unit,
    onMoveExercise: (Int, Int) -> Unit,
    onToggleSet: (Int, Int) -> Unit,
    onCycleSetType: (Int, Int) -> Unit,
    onUpdateSet: (Int, Int, String, String) -> Unit,
    onExerciseClick: (String) -> Unit,
    onSaveExercise: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditingDay by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Tokens.PaddingHorizontal)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Text("←", style = Typography.headlineMedium.copy(color = OffWhite))
            }
            Text(text = day.dayName, style = Typography.labelLarge.copy(color = OffWhite.copy(alpha = 0.6f)))
            Row {
                if (isEditingDay) {
                    TextButton(onClick = onAddExercise) {
                        Text("+ ADD", style = Typography.labelLarge.copy(color = Acid))
                    }
                }
                IconButton(onClick = { isEditingDay = !isEditingDay }) {
                    Text(if (isEditingDay) "DONE" else "EDIT", style = Typography.labelLarge.copy(color = if (isEditingDay) Acid else OffWhite))
                }
            }
        }
        
        Text(
            text = day.splitTitle,
            style = Typography.displayMedium.copy(color = OffWhite, fontWeight = FontWeight.Black)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        if (day.splitTitle == "REST") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "REST DAY", style = Typography.displaySmall.copy(color = OffWhite.copy(alpha = 0.2f)))
            }
        } else {
            val state = rememberReorderableLazyListState(onMove = { from, to ->
                onMoveExercise(from.index, to.index)
            })

            LazyColumn(
                state = state.listState,
                modifier = Modifier.weight(1f).reorderable(state),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                itemsIndexed(day.exercises, key = { _, ex -> ex.id }) { index, exercise ->
                    ReorderableItem(state, key = exercise.id) { isDragging ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Dim)
                                .padding(16.dp)
                        ) {
                            // Exercise Header
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = Acid),
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                )
                            }
                            TextButton(onClick = { onSaveExercise(index) }, contentPadding = PaddingValues(horizontal = 8.dp)) {
                                Text("SAVE", color = Acid, style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            }
                            if (isEditingDay) {
                                IconButton(onClick = { onRemoveExercise(index) }) {
                                    Text("✕", color = Color.Red)
                                }
                                Box(modifier = Modifier.detectReorderAfterLongPress(state).padding(8.dp)) {
                                    Text("☰", style = Typography.headlineMedium.copy(color = Muted))
                                }
                            }
                        }

                        // Target configuration
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                        ) {
                            Text(
                                text = "${exercise.sets} SETS × ${exercise.reps} REPS (Target)", 
                                style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.5f)),
                                modifier = Modifier.weight(1f)
                            )
                            if (exercise.name.lowercase().let { it.contains("bench press") || it.contains("deadlift") || it.contains("squat") }) {
                                Text(
                                    text = "PR: ${exercise.pr} KG", 
                                    style = Typography.labelSmall.copy(color = Acid)
                                )
                            }
                            Text(
                                text = "HISTORY",
                                style = Typography.labelSmall.copy(color = Acid),
                                modifier = Modifier.clickable { onExerciseClick(exercise.name) }
                            )
                        }

                        Divider(color = Muted, thickness = 0.5.dp)
                        
                        // Sets Header
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("SET", modifier = Modifier.width(32.dp), style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.4f)))
                            Text("KG", modifier = Modifier.weight(1f), style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.4f)), textAlign = TextAlign.Center)
                            Text("REPS", modifier = Modifier.weight(1f), style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.4f)), textAlign = TextAlign.Center)
                            Text("DONE", modifier = Modifier.width(48.dp), style = Typography.labelSmall.copy(color = OffWhite.copy(alpha = 0.4f)), textAlign = TextAlign.Center)
                        }

                        // Actual Sets
                        exercise.actualSets.forEachIndexed { setIdx, actualSet ->
                            key(actualSet.id) {
                                Row(
                                    modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .clickable { onCycleSetType(index, setIdx) },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    val setTypeStr = actualSet.setType
                                    if (setTypeStr == "Normal") {
                                        Text("${setIdx + 1}", style = Typography.bodyMedium.copy(color = OffWhite))
                                    } else {
                                        val displayChar = when (setTypeStr) {
                                            "Drop Set" -> "D"
                                            "Super Set" -> "S"
                                            "Warm-up" -> "W"
                                            else -> "${setIdx + 1}"
                                        }
                                        val displayColor = when (setTypeStr) {
                                            "Drop Set" -> Color(0xFFFFA500)
                                            "Super Set" -> Color(0xFF00BFFF)
                                            "Warm-up" -> Color(0xFFFFD700)
                                            else -> OffWhite
                                        }
                                        Text(displayChar, style = Typography.bodyMedium.copy(color = displayColor, fontWeight = FontWeight.Bold))
                                    }
                                }
                                
                                TextField(
                                    value = actualSet.weightInput,
                                    onValueChange = { wInput -> 
                                        onUpdateSet(index, setIdx, wInput, actualSet.repsInput)
                                    },
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = AppBlack,
                                        unfocusedContainerColor = AppBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    ),
                                    textStyle = Typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    singleLine = true,
                                    enabled = !actualSet.isCompleted,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                                
                                TextField(
                                    value = actualSet.repsInput,
                                    onValueChange = { rInput -> 
                                        onUpdateSet(index, setIdx, actualSet.weightInput, rInput)
                                    },
                                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = AppBlack,
                                        unfocusedContainerColor = AppBlack,
                                        focusedTextColor = OffWhite,
                                        unfocusedTextColor = OffWhite
                                    ),
                                    textStyle = Typography.bodyMedium.copy(textAlign = TextAlign.Center),
                                    singleLine = true,
                                    enabled = !actualSet.isCompleted,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .width(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .border(1.dp, if (actualSet.isCompleted) Acid else Muted)
                                            .background(if (actualSet.isCompleted) Acid else Color.Transparent)
                                            .clickable { onToggleSet(index, setIdx) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (actualSet.isCompleted) {
                                            Text("✓", color = AppBlack, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        }
                                    }
                                }
                            }
                            }
                        }

                        // Add/Remove set buttons
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(onClick = { 
                                val ex = exercise.copy(sets = exercise.sets + 1, actualSets = exercise.actualSets + ActualSet())
                                onUpdateExercise(index, ex)
                            }) {
                                Text("+ ADD SET", style = Typography.labelSmall.copy(color = Acid))
                            }
                            if (exercise.actualSets.size > 1) {
                                TextButton(onClick = { 
                                    val ex = exercise.copy(sets = exercise.sets - 1, actualSets = exercise.actualSets.dropLast(1))
                                    onUpdateExercise(index, ex)
                                }) {
                                    Text("- REMOVE SET", style = Typography.labelSmall.copy(color = Color.Red.copy(alpha = 0.8f)))
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
